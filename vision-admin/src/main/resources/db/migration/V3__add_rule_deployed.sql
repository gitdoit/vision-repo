-- Add deployed column to rule table
ALTER TABLE rule ADD COLUMN deployed BOOLEAN DEFAULT FALSE;
