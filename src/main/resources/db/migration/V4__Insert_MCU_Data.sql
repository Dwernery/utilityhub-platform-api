






-- Phase 5 TV Shows
INSERT INTO shows (title, year, phase_id, status, runtime, synopsis, s3_url) VALUES
('Your Friendly Neighborhood Spider-Man', 2025, 5, 'UNWATCHED', NULL, NULL, NULL),
('Daredevil: Born Again', 2025, 5, 'UNWATCHED', NULL, NULL, NULL),
('Ironheart', 2025, 5, 'UNWATCHED', NULL, NULL, NULL);


-- Your Friendly Neighborhood Spider-Man (10 episodes)
((SELECT id FROM shows WHERE title = 'Your Friendly Neighborhood Spider-Man' LIMIT 1), 1, 'Episode 1', 'UNWATCHED'),
((SELECT id FROM shows WHERE title = 'Your Friendly Neighborhood Spider-Man' LIMIT 1), 2, 'Episode 2', 'UNWATCHED'),
((SELECT id FROM shows WHERE title = 'Your Friendly Neighborhood Spider-Man' LIMIT 1), 3, 'Episode 3', 'UNWATCHED'),
((SELECT id FROM shows WHERE title = 'Your Friendly Neighborhood Spider-Man' LIMIT 1), 4, 'Episode 4', 'UNWATCHED'),
((SELECT id FROM shows WHERE title = 'Your Friendly Neighborhood Spider-Man' LIMIT 1), 5, 'Episode 5', 'UNWATCHED'),
((SELECT id FROM shows WHERE title = 'Your Friendly Neighborhood Spider-Man' LIMIT 1), 6, 'Episode 6', 'UNWATCHED'),
((SELECT id FROM shows WHERE title = 'Your Friendly Neighborhood Spider-Man' LIMIT 1), 7, 'Episode 7', 'UNWATCHED'),
((SELECT id FROM shows WHERE title = 'Your Friendly Neighborhood Spider-Man' LIMIT 1), 8, 'Episode 8', 'UNWATCHED'),
((SELECT id FROM shows WHERE title = 'Your Friendly Neighborhood Spider-Man' LIMIT 1), 9, 'Episode 9', 'UNWATCHED'),
((SELECT id FROM shows WHERE title = 'Your Friendly Neighborhood Spider-Man' LIMIT 1), 10, 'Episode 10', 'UNWATCHED'),
-- Daredevil: Born Again Season 1 (9 episodes)
((SELECT id FROM shows WHERE title = 'Daredevil: Born Again' LIMIT 1), 1, 'Episode 1', 'UNWATCHED'),
((SELECT id FROM shows WHERE title = 'Daredevil: Born Again' LIMIT 1), 2, 'Episode 2', 'UNWATCHED'),
((SELECT id FROM shows WHERE title = 'Daredevil: Born Again' LIMIT 1), 3, 'Episode 3', 'UNWATCHED'),
((SELECT id FROM shows WHERE title = 'Daredevil: Born Again' LIMIT 1), 4, 'Episode 4', 'UNWATCHED'),
((SELECT id FROM shows WHERE title = 'Daredevil: Born Again' LIMIT 1), 5, 'Episode 5', 'UNWATCHED'),
((SELECT id FROM shows WHERE title = 'Daredevil: Born Again' LIMIT 1), 6, 'Episode 6', 'UNWATCHED'),
((SELECT id FROM shows WHERE title = 'Daredevil: Born Again' LIMIT 1), 7, 'Episode 7', 'UNWATCHED'),
((SELECT id FROM shows WHERE title = 'Daredevil: Born Again' LIMIT 1), 8, 'Episode 8', 'UNWATCHED'),
((SELECT id FROM shows WHERE title = 'Daredevil: Born Again' LIMIT 1), 9, 'Episode 9', 'UNWATCHED'),
-- Ironheart (6 episodes)
((SELECT id FROM shows WHERE title = 'Ironheart' LIMIT 1), 1, 'Episode 1', 'UNWATCHED'),
((SELECT id FROM shows WHERE title = 'Ironheart' LIMIT 1), 2, 'Episode 2', 'UNWATCHED'),
((SELECT id FROM shows WHERE title = 'Ironheart' LIMIT 1), 3, 'Episode 3', 'UNWATCHED'),
((SELECT id FROM shows WHERE title = 'Ironheart' LIMIT 1), 4, 'Episode 4', 'UNWATCHED'),
((SELECT id FROM shows WHERE title = 'Ironheart' LIMIT 1), 5, 'Episode 5', 'UNWATCHED'),
((SELECT id FROM shows WHERE title = 'Ironheart' LIMIT 1), 6, 'Episode 6', 'UNWATCHED');

-- Phase 6 Movies
INSERT INTO movies (title, year, phase_id, status, runtime, synopsis, s3_url) VALUES
('The Fantastic Four: First Steps', 2025, 6, 'UNWATCHED', NULL, NULL, NULL),
('Spider-Man: Brand New Day', 2026, 6, 'UNWATCHED', NULL, NULL, NULL),
('Avengers: Doomsday', 2026, 6, 'UNWATCHED', NULL, NULL, NULL),
('Avengers: Secret Wars', 2027, 6, 'UNWATCHED', NULL, NULL, NULL);

-- Phase 6 Specials
INSERT INTO specials (title, year, phase_id, status, runtime, synopsis, s3_url) VALUES
('Eyes of Wakanda', 2025, 6, 'UNWATCHED', NULL, NULL, NULL),
('Marvel Zombies', 2025, 6, 'UNWATCHED', NULL, NULL, NULL),
('Wonder Man', 2026, 6, 'UNWATCHED', NULL, NULL, NULL),
('Daredevil: Born Again Season 2', 2027, 6, 'UNWATCHED', NULL, NULL, NULL),
('The Punisher: One Last Kill', 2027, 6, 'UNWATCHED', NULL, NULL, NULL),
('VisionQuest', 2027, 6, 'UNWATCHED', NULL, NULL, NULL);
