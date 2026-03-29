package com.ludwigit.app.controller;

import com.ludwigit.app.dto.requests.CreateShortURLRequestBody;
import com.ludwigit.app.exceptions.AppException;
import com.ludwigit.app.exceptions.ShortedURLNotFoundException;
import com.ludwigit.app.services.ShortedURLService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URISyntaxException;

@RestController
@RequestMapping("/")
public class ShortedURLController {

	private final ShortedURLService shortedUrlService;

	public ShortedURLController(ShortedURLService shortedUrlService) {
		this.shortedUrlService = shortedUrlService;
	}

	@GetMapping(path = "/{shortenUrl}")
	public ResponseEntity<String> retrieveUrl(@PathVariable String shortenUrl) throws URISyntaxException, ShortedURLNotFoundException {
		String originalUrl = shortedUrlService.retrieveUrl(shortenUrl);

		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setLocation(new URI(originalUrl));

		return ResponseEntity
			.status(HttpStatus.MOVED_PERMANENTLY)
			.headers(httpHeaders)
			.build();
	}

	@PostMapping(path = "/create")
	public ResponseEntity<String> shortUrl(
		@Valid @RequestBody CreateShortURLRequestBody body
	) throws AppException {
		String url = body.getUrl();

		return ResponseEntity.ok(shortedUrlService.createShortedURL(url));
	}
}
