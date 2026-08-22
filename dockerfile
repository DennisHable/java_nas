# použijeme oficiální odlehčený obraz Javy 21 pro běh (Alpine Linux)
FROM eclipse-temurin:21-jre-alpine

# nastavíme pracovní adresář uvnitř kontejneru
WORKDIR /app

# zkopírujeme vygenerovaný JAR soubor ze složky 'target' do kontejneru
COPY target/nas-*.jar app.jar

# informujeme Docker, že aplikace uvnitř kontejneru naslouchá na portu 8080
EXPOSE 8080

# příkaz, který kontejner reálně spustí při startu
ENTRYPOINT ["java", "-jar", "app.jar"]
