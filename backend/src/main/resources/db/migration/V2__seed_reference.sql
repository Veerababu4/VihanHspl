-- Reference data deployed to every environment (DESIGN.md §9.4).
-- Branch addresses / phones are PLACEHOLDERS (decision §17 #1): IT Admin replaces them before launch.
-- The bootstrap IT_ADMIN user is created by the application on first start (password from env),
-- never stored here.

INSERT INTO branch (code, name, address, city, district, phone, emergency_phone, email, map_url, opening_hours)
VALUES ('MLG', 'Vihan Multi Speciality Hospital - Miryalaguda',
        'Address to be confirmed', 'Miryalaguda', 'Nalgonda',
        '0000000001', '0000000101', NULL,
        'https://www.google.com/maps?q=Miryalaguda,Telangana&output=embed',
        'OPD: Mon-Sat 9:00 AM - 8:00 PM | Emergency: 24x7');

INSERT INTO branch (code, name, address, city, district, phone, emergency_phone, email, map_url, opening_hours)
VALUES ('HZN', 'Vihan Multi Speciality Hospital - Huzurnagar',
        'Address to be confirmed', 'Huzurnagar', 'Suryapet',
        '0000000002', '0000000102', NULL,
        'https://www.google.com/maps?q=Huzurnagar,Telangana&output=embed',
        'OPD: Mon-Sat 9:00 AM - 8:00 PM | Emergency: 24x7');

INSERT INTO branch (code, name, address, city, district, phone, emergency_phone, email, map_url, opening_hours)
VALUES ('KDD', 'Vihan Multi Speciality Hospital - Kodad',
        'Address to be confirmed', 'Kodad', 'Suryapet',
        '0000000003', '0000000103', NULL,
        'https://www.google.com/maps?q=Kodad,Telangana&output=embed',
        'OPD: Mon-Sat 9:00 AM - 8:00 PM | Emergency: 24x7');

INSERT INTO speciality (name, description) VALUES ('General Medicine',
  'Diagnosis and treatment of fever, infections, diabetes, blood pressure and other adult illnesses.');
INSERT INTO speciality (name, description) VALUES ('General Surgery',
  'Surgical care for hernia, appendix, gall bladder, piles, wounds and abdominal conditions.');
INSERT INTO speciality (name, description) VALUES ('Orthopaedics',
  'Bones, joints and spine: fractures, arthritis, sports injuries and joint replacement.');
INSERT INTO speciality (name, description) VALUES ('Obstetrics & Gynaecology',
  'Pregnancy care, delivery and women''s health.');
INSERT INTO speciality (name, description) VALUES ('Paediatrics',
  'Care for newborns, infants, children and adolescents, including vaccinations.');
INSERT INTO speciality (name, description) VALUES ('Cardiology',
  'Heart care: chest pain, heart attack, ECG, ECHO and blood pressure management.');
INSERT INTO speciality (name, description) VALUES ('Neurology',
  'Brain and nerve disorders: stroke, seizures, headache and paralysis.');
INSERT INTO speciality (name, description) VALUES ('Nephrology',
  'Kidney diseases, dialysis and kidney-related blood pressure problems.');
INSERT INTO speciality (name, description) VALUES ('Urology',
  'Kidney stones, prostate and urinary tract conditions.');
INSERT INTO speciality (name, description) VALUES ('Pulmonology',
  'Lung and breathing problems: asthma, COPD, TB and pneumonia.');
INSERT INTO speciality (name, description) VALUES ('Gastroenterology',
  'Stomach, liver and digestive system disorders, including endoscopy.');
INSERT INTO speciality (name, description) VALUES ('ENT',
  'Ear, nose and throat conditions, sinus and hearing problems.');
INSERT INTO speciality (name, description) VALUES ('Ophthalmology',
  'Eye care: vision tests, cataract and other eye conditions.');
INSERT INTO speciality (name, description) VALUES ('Dermatology',
  'Skin, hair and nail conditions.');
INSERT INTO speciality (name, description) VALUES ('Dental',
  'Dental check-ups, fillings, root canal, extraction and oral surgery.');
INSERT INTO speciality (name, description) VALUES ('Anaesthesiology',
  'Anaesthesia for surgery and pain management.');
INSERT INTO speciality (name, description) VALUES ('Radiology',
  'X-ray, ultrasound and CT imaging.');
INSERT INTO speciality (name, description) VALUES ('Emergency & Critical Care',
  '24x7 emergency treatment, trauma and intensive care.');
