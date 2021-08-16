# Confluence to Google Drive Exporter

This application helps you in exporting your confluence data to your google drive.


# How to run the application with default configurations
## Obtain Google drive refresh token

- Visit Oauth [Playground](https://developers.google.com/oauthplayground/)
- In the left panel choose  *https://www.googleapis.com/auth/drive* under Drive API v3 option
- Authorize with your google account
- Click **Exchange authorization code for token** option under the pre-populated **Authorization code**
- Get the populated **Refresh Token**, this token will be used by the application to upload content in your google drive.
## Run the JAR
- Download the jar file from the repo
- Run the jar file using the below command
- `java -jar confluence-migrator-0.0.1-SNAPSHOT.jar --gDrive.refreshToken="<REFRESH TOKEN OBTAINED USING ABOVE STEPS>"`


# How to compile from source

## Database Configuration


- Provide the SQL database url  in `spring.datasource.url`
- Provide username and password under `spring.datasource.username`, `spring.datasource.password`

## Compile the code
- If you have maven installed use the below command
  - `mvn package`
- If you don't have maven installed, use the maven wrapper provided in the root of this project
- Run the packaged JAR by following [these](#How-to-run-the-application-with-default-configurations) steps.

## How to export
- Send an GET request to the below url using Chrome / any API client
    - `localhost:8000/process?spaceKey=<SPACE KEY>&uploadGdrive=true`
- If you just wanna export the space to your local filesystem fire the below request
    - `localhost:8000/process?spaceKey=<SPACE KEY>&uploadGdrive=false`
- If you found some page/content is missing after exporting, you can fire the below request to retry exporting failed or incomplete page
    - `localhost:8000/retry/<SPACE KEY>`
- Get the migration status by firing an request to the below url
    - `localhost:8000/space/<SPACE KEY>/report`
- If you want to export all the spaces under your domain, send an request to
    - `localhost:8000/process?uploadGdrive=true`
