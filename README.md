# springweb
 
docker run --name mysql-container -e MYSQL_ROOT_PASSWORD=yourpassword -p 3306:3306 -d mysql:latest
docker exec -it mysql-container mysql -uroot -p
CREATE DATABASE test_db;
SHOW DATABASES;

