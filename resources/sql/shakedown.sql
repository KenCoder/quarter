--name: create-tables!
CREATE TABLE IF NOT EXISTS shakedowns (
    id serial,
    scout_name varchar(100),
    site varchar(100),
    shake_date varchar(20),
    gear jsonb
);


--name: drop-tables!
DROP TABLE IF EXISTS shakedowns;

--name: get-shakedown
SELECT shakedown from shakedowns where id=:id;

--name: get-shakedowns
SELECT scout_name AS name, site, shake_date AS "date", gear::text FROM shakedowns ORDER BY shake_date DESC;

--name: get-shakedowns-by-name-date
SELECT scout_name AS name, site, shake_date AS "date", gear::text FROM shakedowns
WHERE scout_name = :name AND shake_date = :date AND site = :site
ORDER BY shake_date DESC;

--name: insert-shakedown<!
INSERT INTO shakedowns (scout_name, site, shake_date, gear) values(:name, :site, :date, :gear::jsonb);

--name: update-shakedown!
INSERT INTO shakedowns set scout_name = :name, site = :site,
    shake_date = :date, gear =:gear::jsonb WHERE id=:id;
