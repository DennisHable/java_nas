CREATE DATABASE nas ENCODING 'UTF8'; -- vytvoření db; znaková sada
GRANT ALL PRIVILEGES ON DATABASE nas TO admin1; -- udělení všech oprávnění; všechny tabulky v db nas
GRANT ALL ON SCHEMA public TO admin1; -- uživ. musí mít oprávnění i do public, jinak v nový verzích Postgre nemůže vytvářet (Hibernate) tabulky
