INSERT INTO rooms (name, theme, description, difficulty, min_players, max_players,
                   session_minutes, reset_minutes, price_per_person)
VALUES
    ('L''Asile de Saint-Ombre', 'Hôpital psychiatrique',
     'Fermé en 1962, l''asile n''a jamais vraiment été vidé. Les dossiers des patients cachent la clé de votre sortie.',
     3, 2, 5, 60, 30, 30.00),

    ('La Chambre des Poupées', 'Maison hantée',
     'Une nursery figée dans le temps. Les poupées ne sont jamais à la même place quand on se retourne.',
     2, 2, 4, 60, 30, 28.00),

    ('Le Rituel', 'Secte',
     'Dans le sous-sol d''une secte, une cérémonie se prépare. Interrompez-la avant minuit.',
     4, 3, 6, 60, 30, 32.00),

    ('Laboratoire 13', 'Expérience scientifique',
     'Une expérience a mal tourné. Quelque chose s''est échappé, et vous êtes enfermés avec.',
     5, 4, 6, 75, 30, 35.00);