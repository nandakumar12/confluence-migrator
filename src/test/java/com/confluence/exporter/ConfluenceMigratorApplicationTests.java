package com.confluence.exporter;

import com.confluence.exporter.config.AppConfig;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.DriveScopes;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;


class ConfluenceMigratorApplicationTests {

	String refreshToken = "1//04ubHBNaDdKm-CgYIARAAGAQSNwF-L9IrtNBXzWqMMRa05Eim5K-b2iLLeOwnf3qsrmEITgn6Gjljz3XYJoqvFhS8M9TkMknfnRQ";

	private static final String CREDENTIALS_FILE_PATH = "/credentials.json";
	private static final JacksonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
	private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE);


	@Test
	void contextLoads() throws GeneralSecurityException, IOException {

		final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

		InputStream in = AppConfig.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
		if (in == null) {
			throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
		}
		GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

		String clientId = clientSecrets.getDetails().getClientId();
		String clientSecret = clientSecrets.getDetails().getClientSecret();

		TokenResponse tokenResponse = new GoogleRefreshTokenRequest(new NetHttpTransport(), new JacksonFactory(),
				refreshToken, clientId, clientSecret).setScopes(SCOPES).setGrantType("refresh_token").execute();

		String accessToken = tokenResponse.getAccessToken();

		GoogleCredential credential =   new GoogleCredential.Builder()
				.setTransport(HTTP_TRANSPORT)
				.setJsonFactory(JSON_FACTORY)
				.setClientSecrets(clientId, clientSecret)
				.build();

		credential.setAccessToken(accessToken);
		credential.setRefreshToken(refreshToken);


		System.out.println("Access Token: " + credential.getAccessToken());
		System.out.println(credential.getExpiresInSeconds());
		System.out.println("RefreshToken: " +credential.getRefreshToken());

		credential.refreshToken();

		System.out.println("Refreshed Access Token: " +credential.getAccessToken());
		System.out.println(credential.getExpiresInSeconds());
		System.out.println("RefreshToken: " +credential.getRefreshToken());

	}

}
