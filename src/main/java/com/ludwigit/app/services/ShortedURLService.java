package com.ludwigit.app.services;

import com.ludwigit.app.config.AppConfig;
import com.ludwigit.app.exceptions.InvalidURLException;
import com.ludwigit.app.exceptions.ShortedURLNotFoundException;
import com.ludwigit.app.model.ShortedURL;
import com.ludwigit.app.repositories.ShortedURLRepository;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class ShortedURLService {

	private final HashIdsService hashIdsService;
	private final ShortedURLRepository shortedUrlRepository;
	private final RedisTemplate<String, Object> redisTemplate;
	private final URI baseUri;

	public ShortedURLService(
		ShortedURLRepository shortedUrlRepository,
		HashIdsService hashIdsService,
		AppConfig appConfig,
		RedisTemplate<String, Object> redisTemplate
	) {
		this.hashIdsService = hashIdsService;
		this.shortedUrlRepository = shortedUrlRepository;
		this.baseUri = URI.create(appConfig.getBaseUrl());
		this.redisTemplate = redisTemplate;
	}

	public String createShortedURL(String originalUrl) throws InvalidURLException {
		boolean isFromTheSameAppDomain = Objects.equals(
			URI.create(originalUrl).getHost(),
			baseUri.getHost()
		);

		if (isFromTheSameAppDomain) {
			throw new InvalidURLException("This is so silly, you cannot shorten a URL from the same domain as the app ;)");
		}

		// Create a new shorted URL
		ShortedURL newShortedUrl = ShortedURL.builder()
			.originalUrl(originalUrl)
			.build();

		ShortedURL shortedUrl = shortedUrlRepository.save(newShortedUrl);
		String obfuscatedBase62URL = hashIdsService.encode(shortedUrl.getId());

		String cacheKey = "shortedUrls:" + obfuscatedBase62URL;

		// Cache the shorted URL for 6 hours
		redisTemplate.opsForValue().set(cacheKey, shortedUrl, 6, TimeUnit.HOURS);

		return this.baseUri.resolve(obfuscatedBase62URL).toString();
	}

	public String retrieveUrl(@NotNull String shortedURL) throws ShortedURLNotFoundException {
		Long decodedId = hashIdsService
			.decode(shortedURL)
			// Do not show the exact error message to the user, as it may contain sensitive information about the hashids configuration
			.orElseThrow(ShortedURLNotFoundException::new);

		String cacheKey = "shortedUrls:" + shortedURL;
		ShortedURL cachedShortedUrl = (ShortedURL) redisTemplate.opsForValue().get(cacheKey);

		if (cachedShortedUrl != null) {
			return cachedShortedUrl.getOriginalUrl();
		}

		Optional<ShortedURL> shortedURLObject = shortedUrlRepository.findById(decodedId);

		if (shortedURLObject.isEmpty()) {
			throw new ShortedURLNotFoundException();
		}

		// Cache the shorted URL for 6 hours
		redisTemplate.opsForValue().set(cacheKey, shortedURLObject.get(), 6, TimeUnit.HOURS);

		return shortedURLObject.get().getOriginalUrl();
	}
}
