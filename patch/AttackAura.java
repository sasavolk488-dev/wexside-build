name: Build WexSide Client

on:
  push:
    branches: [ "main", "master" ]
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Find and Unzip source code
        run: |
          ZIP_FILE="1793782978035.zip"
          if [ ! -f "$ZIP_FILE" ]; then
            ZIP_FILE=$(find . -maxdepth 1 -name "*.zip" | head -n 1)
          fi
          unzip "$ZIP_FILE" -d project_source

      - name: Apply AttackAura Patch
        run: |
          # Находим старую ауру и заменяем на нашу новую
          find project_source -name "AttackAura.java" -exec cp patch/AttackAura.java {} \;
          echo "Файл AttackAura.java успешно заменен!"

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v4

      - name: Build with Gradle
        run: |
          cd project_source/WexSideSRC
          chmod +x gradlew
          ./gradlew build

      - name: Upload JAR
        uses: actions/upload-artifact@v4
        with:
          name: wexside-jar
          path: project_source/WexSideSRC/build/libs/*.jar
