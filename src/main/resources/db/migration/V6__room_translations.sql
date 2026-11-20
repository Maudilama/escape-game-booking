-- Traductions du thème et de la description des salles.
-- Le français reste dans la table rooms : c'est la langue de référence et la langue de repli.
-- Le nom d'une salle est un nom propre : il ne se traduit pas.
CREATE TABLE room_translations (
    room_id     BIGINT       NOT NULL REFERENCES rooms (id) ON DELETE CASCADE,
    locale      VARCHAR(5)   NOT NULL,
    theme       VARCHAR(100) NOT NULL,
    description TEXT         NOT NULL,
    PRIMARY KEY (room_id, locale)
);

-- On retrouve chaque salle par son nom (unique) plutôt que par un id qui dépend de l'ordre d'insertion.
INSERT INTO room_translations (room_id, locale, theme, description)
SELECT r.id, t.locale, t.theme, t.description
FROM (VALUES
    ('L''Asile de Saint-Ombre', 'en', 'Psychiatric hospital',
     'Closed in 1962, the asylum was never really emptied. The patients'' files hide the key to your way out.'),
    ('L''Asile de Saint-Ombre', 'es', 'Hospital psiquiátrico',
     'Cerrado en 1962, el manicomio nunca llegó a vaciarse del todo. Los expedientes de los pacientes esconden la llave de vuestra salida.'),

    ('La Chambre des Poupées', 'en', 'Haunted house',
     'A nursery frozen in time. The dolls are never in the same place when you turn around.'),
    ('La Chambre des Poupées', 'es', 'Casa encantada',
     'Un cuarto infantil detenido en el tiempo. Las muñecas nunca están en el mismo sitio cuando os dais la vuelta.'),

    ('Le Rituel', 'en', 'Cult',
     'In the basement of a cult, a ceremony is being prepared. Stop it before midnight.'),
    ('Le Rituel', 'es', 'Secta',
     'En el sótano de una secta se prepara una ceremonia. Detenedla antes de medianoche.'),

    ('Laboratoire 13', 'en', 'Scientific experiment',
     'An experiment went wrong. Something escaped, and you are locked in with it.'),
    ('Laboratoire 13', 'es', 'Experimento científico',
     'Un experimento ha salido mal. Algo se ha escapado, y estáis encerrados con ello.')
) AS t (room_name, locale, theme, description)
JOIN rooms r ON r.name = t.room_name;