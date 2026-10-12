INSERT INTO category (name, description) VALUES ('Informatique', 'Ordinateurs et accessoires');
INSERT INTO category (name, description) VALUES ('Livres', 'Romans, BD, essais');

INSERT INTO product (name, description, price, stock, category_id, version) VALUES ('Clavier mécanique', 'Switches rouges', 89.90, 20, 1, 0);
INSERT INTO product (name, description, price, stock, category_id, version) VALUES ('Souris sans fil', 'Ergonomique', 29.99, 50, 1, 0);
INSERT INTO product (name, description, price, stock, category_id, version) VALUES ('Clean Code', 'Robert C. Martin', 35.00, 10, 2, 0);

-- Les clients (avec mot de passe haché BCrypt) sont créés au démarrage par security/DemoDataInitializer.
