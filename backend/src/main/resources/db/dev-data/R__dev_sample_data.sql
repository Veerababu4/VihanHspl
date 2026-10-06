-- LOCAL-ONLY SAMPLE DATA (DESIGN.md §9.4). Loaded only by the `local` profile; never deployed.
-- All doctors are FICTIONAL. Registration numbers start with SAMPLE- so they can't be mistaken for real ones.
-- Repeatable migration: re-runs whenever this file changes, so every statement is idempotent (MERGE / NOT EXISTS).
-- Staff users, patients, slots, appointments and visits need app code (BCrypt, Aadhaar crypto, slot generator)
-- and are seeded by LocalDataSeeder in the milestone that builds that code (M3, M4/M5, M7, M8).

-- ===== Doctors =====
MERGE INTO doctor d
USING (
  SELECT 'SAMPLE-001' reg_no, 'Dr. Srinivas Kandula' full_name, 'M' gender, 'General Medicine' spec,
         'MBBS, MD (General Medicine)' qual, 'Senior Consultant' desig, 2004 start_year, 500 fee FROM dual UNION ALL
  SELECT 'SAMPLE-002', 'Dr. Lakshmi Prasanna Bollam', 'F', 'General Surgery',
         'MBBS, MS (General Surgery)', 'Consultant', 2008, 500 FROM dual UNION ALL
  SELECT 'SAMPLE-003', 'Dr. Ravi Teja Gaddam', 'M', 'Orthopaedics',
         'MBBS, MS (Ortho)', 'Consultant', 2010, 600 FROM dual UNION ALL
  SELECT 'SAMPLE-004', 'Dr. Swathi Nalla', 'F', 'Obstetrics & Gynaecology',
         'MBBS, MS (OBG)', 'Senior Consultant', 2009, 500 FROM dual UNION ALL
  SELECT 'SAMPLE-005', 'Dr. Mahesh Chityala', 'M', 'Paediatrics',
         'MBBS, MD (Paediatrics)', 'Consultant', 2012, 400 FROM dual UNION ALL
  SELECT 'SAMPLE-006', 'Dr. Venkata Ramana Pulla', 'M', 'Cardiology',
         'MBBS, MD, DM (Cardiology)', 'Chief Cardiologist', 2001, 800 FROM dual UNION ALL
  SELECT 'SAMPLE-007', 'Dr. Anitha Gundla', 'F', 'Neurology',
         'MBBS, MD, DM (Neurology)', 'Consultant', 2011, 700 FROM dual UNION ALL
  SELECT 'SAMPLE-008', 'Dr. Kiran Kumar Yerra', 'M', 'Nephrology',
         'MBBS, MD, DM (Nephrology)', 'Consultant', 2013, 700 FROM dual UNION ALL
  SELECT 'SAMPLE-009', 'Dr. Sai Krishna Mallela', 'M', 'Urology',
         'MBBS, MS, MCh (Urology)', 'Consultant', 2014, 700 FROM dual UNION ALL
  SELECT 'SAMPLE-010', 'Dr. Padmaja Thota', 'F', 'Pulmonology',
         'MBBS, MD (Pulmonary Medicine)', 'Consultant', 2010, 500 FROM dual UNION ALL
  SELECT 'SAMPLE-011', 'Dr. Naveen Bairi', 'M', 'Gastroenterology',
         'MBBS, MD, DM (Gastroenterology)', 'Consultant', 2012, 700 FROM dual UNION ALL
  SELECT 'SAMPLE-012', 'Dr. Haritha Kommu', 'F', 'ENT',
         'MBBS, MS (ENT)', 'Consultant', 2015, 400 FROM dual UNION ALL
  SELECT 'SAMPLE-013', 'Dr. Prakash Dasari', 'M', 'Ophthalmology',
         'MBBS, MS (Ophthalmology)', 'Senior Consultant', 2007, 400 FROM dual UNION ALL
  SELECT 'SAMPLE-014', 'Dr. Divya Sree Pendem', 'F', 'Dermatology',
         'MBBS, MD (DVL)', 'Consultant', 2016, 500 FROM dual UNION ALL
  SELECT 'SAMPLE-015', 'Dr. Rajesh Vuppala', 'M', 'Dental',
         'BDS, MDS (Oral & Maxillofacial Surgery)', 'Consultant', 2011, 300 FROM dual UNION ALL
  SELECT 'SAMPLE-016', 'Dr. Sunitha Mekala', 'F', 'Anaesthesiology',
         'MBBS, MD (Anaesthesia)', 'Senior Consultant', 2006, 500 FROM dual UNION ALL
  SELECT 'SAMPLE-017', 'Dr. Vamshi Krishna Ponnala', 'M', 'Radiology',
         'MBBS, MD (Radio-Diagnosis)', 'Consultant', 2013, 500 FROM dual UNION ALL
  SELECT 'SAMPLE-018', 'Dr. Ashok Nerella', 'M', 'Emergency & Critical Care',
         'MBBS, MD (Emergency Medicine)', 'Head - Emergency', 2014, 500 FROM dual UNION ALL
  SELECT 'SAMPLE-019', 'Dr. Bhavani Jakkula', 'F', 'General Medicine',
         'MBBS, MD (General Medicine)', 'Consultant', 2015, 400 FROM dual UNION ALL
  SELECT 'SAMPLE-020', 'Dr. Ramesh Banoth', 'M', 'Paediatrics',
         'MBBS, DCH, DNB (Paediatrics)', 'Consultant', 2017, 400 FROM dual
) s
ON (d.registration_no = s.reg_no)
WHEN MATCHED THEN UPDATE SET
  d.full_name = s.full_name, d.gender = s.gender,
  d.speciality_id = (SELECT speciality_id FROM speciality WHERE name = s.spec),
  d.qualification = s.qual, d.designation = s.desig, d.practice_start_year = s.start_year,
  d.consultation_fee = s.fee
WHEN NOT MATCHED THEN INSERT
  (registration_no, full_name, gender, speciality_id, qualification, designation, practice_start_year,
   languages, bio, consultation_fee)
VALUES
  (s.reg_no, s.full_name, s.gender, (SELECT speciality_id FROM speciality WHERE name = s.spec), s.qual, s.desig,
   s.start_year, 'Telugu, English, Hindi',
   'Sample profile of a fictional doctor, for local testing only.', s.fee);

-- ===== Doctor <-> branch =====
-- Day mask = Mon..Sun ('1' = works that day). Two-branch doctors use non-overlapping days.
-- The same list drives both doctor_branch and doctor_schedule below.
INSERT INTO doctor_branch (doctor_id, branch_id)
WITH plan AS (
  SELECT 'SAMPLE-001' reg_no, 'MLG' branch, '1010100' days FROM dual UNION ALL
  SELECT 'SAMPLE-001', 'HZN', '0101010' FROM dual UNION ALL
  SELECT 'SAMPLE-002', 'MLG', '1111110' FROM dual UNION ALL
  SELECT 'SAMPLE-003', 'MLG', '1010100' FROM dual UNION ALL
  SELECT 'SAMPLE-003', 'KDD', '0101010' FROM dual UNION ALL
  SELECT 'SAMPLE-004', 'HZN', '1111110' FROM dual UNION ALL
  SELECT 'SAMPLE-005', 'KDD', '1111110' FROM dual UNION ALL
  SELECT 'SAMPLE-006', 'MLG', '1001000' FROM dual UNION ALL
  SELECT 'SAMPLE-006', 'HZN', '0100100' FROM dual UNION ALL
  SELECT 'SAMPLE-006', 'KDD', '0010010' FROM dual UNION ALL
  SELECT 'SAMPLE-007', 'MLG', '1111110' FROM dual UNION ALL
  SELECT 'SAMPLE-008', 'KDD', '1111100' FROM dual UNION ALL
  SELECT 'SAMPLE-009', 'HZN', '1010100' FROM dual UNION ALL
  SELECT 'SAMPLE-009', 'KDD', '0101010' FROM dual UNION ALL
  SELECT 'SAMPLE-010', 'HZN', '1111110' FROM dual UNION ALL
  SELECT 'SAMPLE-011', 'MLG', '1111100' FROM dual UNION ALL
  SELECT 'SAMPLE-012', 'KDD', '1111110' FROM dual UNION ALL
  SELECT 'SAMPLE-013', 'HZN', '1111110' FROM dual UNION ALL
  SELECT 'SAMPLE-014', 'MLG', '1010100' FROM dual UNION ALL
  SELECT 'SAMPLE-015', 'KDD', '1111110' FROM dual UNION ALL
  SELECT 'SAMPLE-016', 'MLG', '1111110' FROM dual UNION ALL
  SELECT 'SAMPLE-017', 'HZN', '1111110' FROM dual UNION ALL
  SELECT 'SAMPLE-018', 'KDD', '1111111' FROM dual UNION ALL
  SELECT 'SAMPLE-019', 'KDD', '1111110' FROM dual UNION ALL
  SELECT 'SAMPLE-020', 'MLG', '1010100' FROM dual UNION ALL
  SELECT 'SAMPLE-020', 'HZN', '0101010' FROM dual
)
SELECT d.doctor_id, b.branch_id
FROM plan p
JOIN doctor d ON d.registration_no = p.reg_no
JOIN branch b ON b.code = p.branch
WHERE NOT EXISTS (SELECT 1 FROM doctor_branch x WHERE x.doctor_id = d.doctor_id AND x.branch_id = b.branch_id);

-- ===== Weekly schedules (20-min slots) =====
-- Morning OPD 10:00-14:00 on the doctor's days; doctors marked 'E' also have an evening OPD 17:00-19:00.
INSERT INTO doctor_schedule (doctor_id, branch_id, day_of_week, start_time, end_time, slot_minutes, effective_from)
WITH plan AS (
  SELECT 'SAMPLE-001' reg_no, 'MLG' branch, '1010100' days, 'E' evening FROM dual UNION ALL
  SELECT 'SAMPLE-001', 'HZN', '0101010', 'E' FROM dual UNION ALL
  SELECT 'SAMPLE-002', 'MLG', '1111110', '-' FROM dual UNION ALL
  SELECT 'SAMPLE-003', 'MLG', '1010100', '-' FROM dual UNION ALL
  SELECT 'SAMPLE-003', 'KDD', '0101010', '-' FROM dual UNION ALL
  SELECT 'SAMPLE-004', 'HZN', '1111110', 'E' FROM dual UNION ALL
  SELECT 'SAMPLE-005', 'KDD', '1111110', 'E' FROM dual UNION ALL
  SELECT 'SAMPLE-006', 'MLG', '1001000', '-' FROM dual UNION ALL
  SELECT 'SAMPLE-006', 'HZN', '0100100', '-' FROM dual UNION ALL
  SELECT 'SAMPLE-006', 'KDD', '0010010', '-' FROM dual UNION ALL
  SELECT 'SAMPLE-007', 'MLG', '1111110', '-' FROM dual UNION ALL
  SELECT 'SAMPLE-008', 'KDD', '1111100', '-' FROM dual UNION ALL
  SELECT 'SAMPLE-009', 'HZN', '1010100', '-' FROM dual UNION ALL
  SELECT 'SAMPLE-009', 'KDD', '0101010', '-' FROM dual UNION ALL
  SELECT 'SAMPLE-010', 'HZN', '1111110', '-' FROM dual UNION ALL
  SELECT 'SAMPLE-011', 'MLG', '1111100', '-' FROM dual UNION ALL
  SELECT 'SAMPLE-012', 'KDD', '1111110', 'E' FROM dual UNION ALL
  SELECT 'SAMPLE-013', 'HZN', '1111110', '-' FROM dual UNION ALL
  SELECT 'SAMPLE-014', 'MLG', '1010100', 'E' FROM dual UNION ALL
  SELECT 'SAMPLE-015', 'KDD', '1111110', 'E' FROM dual UNION ALL
  SELECT 'SAMPLE-016', 'MLG', '1111110', '-' FROM dual UNION ALL
  SELECT 'SAMPLE-017', 'HZN', '1111110', '-' FROM dual UNION ALL
  SELECT 'SAMPLE-018', 'KDD', '1111111', 'E' FROM dual UNION ALL
  SELECT 'SAMPLE-019', 'KDD', '1111110', 'E' FROM dual UNION ALL
  SELECT 'SAMPLE-020', 'MLG', '1010100', '-' FROM dual UNION ALL
  SELECT 'SAMPLE-020', 'HZN', '0101010', '-' FROM dual
),
weekday AS (SELECT LEVEL dow FROM dual CONNECT BY LEVEL <= 7),
session_time AS (
  SELECT 'M' kind, '10:00' start_time, '14:00' end_time FROM dual UNION ALL
  SELECT 'E', '17:00', '19:00' FROM dual
),
wanted AS (
  SELECT d.doctor_id, b.branch_id, w.dow, t.start_time, t.end_time
  FROM plan p
  JOIN doctor d ON d.registration_no = p.reg_no
  JOIN branch b ON b.code = p.branch
  JOIN weekday w ON SUBSTR(p.days, w.dow, 1) = '1'
  JOIN session_time t ON t.kind = 'M' OR (t.kind = 'E' AND p.evening = 'E')
)
SELECT doctor_id, branch_id, dow, start_time, end_time, 20, DATE '2026-01-01'
FROM wanted n
WHERE NOT EXISTS (
  SELECT 1 FROM doctor_schedule x
  WHERE x.doctor_id = n.doctor_id AND x.branch_id = n.branch_id
    AND x.day_of_week = n.dow AND x.start_time = n.start_time);

-- ===== Placeholder images (served by the frontend from /placeholders; no real photos) =====
UPDATE branch SET image_url = '/placeholders/branch.svg' WHERE image_url IS NULL;

INSERT INTO gallery_image (branch_id, title, caption, image_url, thumb_url, sort_order)
WITH g AS (
  SELECT 'MLG' branch, 'Reception' title, 'Placeholder image' caption, '/placeholders/reception.svg' img, 1 sort_order FROM dual UNION ALL
  SELECT 'MLG', 'Operation theatre', 'Placeholder image', '/placeholders/ot.svg', 2 FROM dual UNION ALL
  SELECT 'HZN', 'OPD waiting area', 'Placeholder image', '/placeholders/opd.svg', 3 FROM dual UNION ALL
  SELECT 'HZN', 'Laboratory', 'Placeholder image', '/placeholders/lab.svg', 4 FROM dual UNION ALL
  SELECT 'KDD', 'Patient ward', 'Placeholder image', '/placeholders/ward.svg', 5 FROM dual UNION ALL
  SELECT 'KDD', 'Pharmacy', 'Placeholder image', '/placeholders/pharmacy.svg', 6 FROM dual
)
SELECT b.branch_id, g.title, g.caption, g.img, g.img, g.sort_order
FROM g JOIN branch b ON b.code = g.branch
WHERE NOT EXISTS (SELECT 1 FROM gallery_image x WHERE x.image_url = g.img AND x.branch_id = b.branch_id);
