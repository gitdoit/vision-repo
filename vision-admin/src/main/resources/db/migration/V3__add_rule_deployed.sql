-- Add deployed column to rule table
ALTER TABLE vision.rule ADD COLUMN deployed BOOLEAN DEFAULT FALSE;
