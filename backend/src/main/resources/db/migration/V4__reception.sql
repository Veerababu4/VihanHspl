-- M6 reception & dashboard (DESIGN.md §5.5, §5.6).

-- "Patient registrations" report groups new UHIDs by day.
CREATE INDEX ix_patient_created ON patient (created_at);

-- Reception's "needs action" call list.
CREATE INDEX ix_appt_needs_action ON appointment (needs_action, appt_date);
