-- Add label column to camera table for storing video platform label/tag name
ALTER TABLE vision.camera ADD COLUMN label VARCHAR(200);
