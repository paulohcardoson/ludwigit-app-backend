package com.ludwigit.app.services;

import com.ludwigit.app.config.AppConfig;
import com.ludwigit.app.config.HashIdConfig;
import com.ludwigit.app.exceptions.InvalidURLException;
import com.ludwigit.app.exceptions.ShortedURLNotFoundException;
import com.ludwigit.app.model.ShortedURL;
import com.ludwigit.app.repositories.ShortedURLRepository;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.net.URI;
import java.util.Optional;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
	HashIdConfig.class,
	AppConfig.class,
	HashIdsService.class,
	ShortedURLService.class
})
@TestPropertySource(properties = {
	"app.hashids.salt=my-secret-salt",
	"app.hashids.min-length=4",
	"app.base-url=http://localhost:3333/"
})
public class ShortedURLServiceTest {

	@MockitoBean
	private ShortedURLRepository shortedURLRepository;

	@MockitoBean
	private RedisTemplate<String, Object> redisTemplate;
	@MockitoBean
	private ValueOperations<String, Object> valueOperations;

	@Autowired
	private HashIdsService hashIdsService;

	@Autowired
	private ShortedURLService shortedURLService;

	@BeforeEach
	public void setUp() {
		Mockito.when(redisTemplate.opsForValue()).thenReturn(valueOperations);

		// Return null for any get operation to simulate cache miss
		Mockito.when(valueOperations.get(Mockito.anyString())).thenReturn(null);
	}

	// Criar uma URL encurtada
	@SneakyThrows
	@Test
	@DisplayName("Deve criar uma URL encurtada a partir de uma URL original válida")
	public void createShortedURLTest1() {
		String originalUrl = "https://www.google.com";

		Mockito.when(shortedURLRepository.save(Mockito.any(ShortedURL.class))).thenReturn(
			ShortedURL.builder()
				.id(1L)
				.originalUrl(originalUrl)
				.build()
		);

		Assertions.assertEquals(
			URI.create("http://localhost:3333/").resolve(hashIdsService.encode(1L)).toString(),
			shortedURLService.createShortedURL(originalUrl)
		);
	}

	@Test
	@DisplayName("Deve lançar InvalidURLException ao tentar criar uma URL encurtada para uma URL original do mesmo domínio da aplicação")
	public void createShortedURLTest3() {
		String originalUrl = "http://localhost:3333/some-path";

		Assertions.assertThrows(
			InvalidURLException.class,
			() -> shortedURLService.createShortedURL(originalUrl)
		);
	}

	//	Recuperar URL original a partir de uma URL encurtada
	@SneakyThrows
	@Test
	@DisplayName("Deve recuperar a URL original a partir de uma URL encurtada válida")
	public void retrieveUrlTest1() {
		String originalUrl = "https://www.google.com";
		Long id = 1L;
		String shortedUrl = hashIdsService.encode(id);

		Mockito.when(shortedURLRepository.findById(id)).thenReturn(
			Optional.of(
				ShortedURL.builder()
					.id(id)
					.originalUrl(originalUrl)
					.build()
			)
		);

		Assertions.assertEquals(
			originalUrl,
			shortedURLService.retrieveUrl(shortedUrl)
		);
	}

	@SneakyThrows
	@Test
	@DisplayName("Deve recuperar a URL original a partir de uma URL encurtada válida presente no cache")
	public void retrieveUrlTest2() {
		String originalUrl = "https://www.google.com";
		Long id = 1L;
		String shortedUrl = hashIdsService.encode(id);

		Mockito.when(valueOperations.get(Mockito.anyString())).thenReturn(
			ShortedURL.builder()
				.id(id)
				.originalUrl(originalUrl)
				.build()
		);

		Assertions.assertEquals(
			originalUrl,
			shortedURLService.retrieveUrl(shortedUrl)
		);
	}

	@Test
	@DisplayName("Deve lançar ShortedURLNotFoundException ao tentar recuperar a URL original a partir de uma URL encurtada que não existe")
	public void retrieveUrlTest3() {
		Long id = 1L;
		String shortedUrl = hashIdsService.encode(id);

		Mockito.when(shortedURLRepository.findById(id)).thenReturn(Optional.empty());

		Assertions.assertThrows(
			ShortedURLNotFoundException.class,
			() -> shortedURLService.retrieveUrl(shortedUrl)
		);
	}

	@Test
	@DisplayName("Deve lançar ShortedURLNotFoundException ao tentar recuperar a URL original a partir de uma URL encurtada com um ID inválido")
	public void retrieveUrlTest4() {
		String shortedUrl = "invalidShortedUrl";

		Assertions.assertThrows(
			ShortedURLNotFoundException.class,
			() -> shortedURLService.retrieveUrl(shortedUrl)
		);
	}

}
