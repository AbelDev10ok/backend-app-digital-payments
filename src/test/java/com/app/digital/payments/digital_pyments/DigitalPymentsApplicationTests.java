package com.app.digital.payments.digital_pyments;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import jakarta.persistence.EntityManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.app.digital.payments.digital_pyments.models.Business;
import com.app.digital.payments.digital_pyments.models.Product;
import com.app.digital.payments.digital_pyments.models.ProductType;
import com.app.digital.payments.digital_pyments.models.dtos.AttributeDto;
import com.app.digital.payments.digital_pyments.models.dtos.AttributeValueDto;
import com.app.digital.payments.digital_pyments.models.dtos.ProductDto;
import com.app.digital.payments.digital_pyments.models.dtos.ProductRequestDto;
import com.app.digital.payments.digital_pyments.models.dtos.ProductVariantRequestDto;
import com.app.digital.payments.digital_pyments.repositories.IBusinessRepository;
import com.app.digital.payments.digital_pyments.repositories.IProductRepository;
import com.app.digital.payments.digital_pyments.repositories.IProductTypeRepository;
import com.app.digital.payments.digital_pyments.repositories.ISaleRepository;
import com.app.digital.payments.digital_pyments.services.AttributeServices;
import com.app.digital.payments.digital_pyments.services.ProductServices;
import com.app.digital.payments.digital_pyments.utils.TenantContext;

/**
 * Prueba de integracion: levanta el contexto completo contra un Postgres real.
 * Se salta automaticamente si Docker no esta disponible (disabledWithoutDocker).
 * Valida que el esquema generado por las entidades
 * (spring.jpa.hibernate.ddl-auto=update) quede listo para operacion y que
 * editar un producto con atributos no rompa las claves unicas del catalogo.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class DigitalPymentsApplicationTests {

	@Container
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

	@DynamicPropertySource
	static void databaseProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
	}

	@Autowired
	private ISaleRepository saleRepository;

	@Autowired
	private ProductServices productServices;

	@Autowired
	private AttributeServices attributeServices;

	@Autowired
	private IBusinessRepository businessRepository;

	@Autowired
	private IProductTypeRepository productTypeRepository;

	@Autowired
	private IProductRepository productRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void contextLoadsWithSchemaFromEntities() {
		assertThat(saleRepository.count()).isZero();
	}

	@Test
	@Transactional
	void editarUnProductoConAtributosNoRompeLasClavesUnicas() {
		Business negocio = businessRepository.save(new Business("Negocio IT", "ARS"));
		TenantContext.setBusinessId(negocio.getId());
		try {
			ProductType categoria = new ProductType();
			categoria.setName("Categoria IT");
			categoria.setSlug("categoria-it");
			categoria.setBusiness(negocio);
			productTypeRepository.save(categoria);

			AttributeDto atributo = attributeServices.create(new AttributeDto(null, "Ram IT",
					List.of(new AttributeValueDto(null, "4"), new AttributeValueDto(null, "8"))));
			Long valor4 = atributo.getValues().get(0).getId();
			Long valor8 = atributo.getValues().get(1).getId();

			ProductRequestDto alta = new ProductRequestDto();
			alta.setName("Producto IT");
			alta.setProductTypeId(categoria.getId());
			alta.setAttributeIds(List.of(atributo.getId()));
			alta.setVariants(List.of(variante(null, valor4)));
			List<ProductDto> creados = productServices.createProduct(alta);
			Long productoId = creados.get(0).getId();
			Long varianteId = creados.get(0).getVariantId();
			entityManager.flush();

			// Mismo valor de atributo: antes fallaba con "llave duplicada ...
			// uk_variant_attribute_variant_value" al insertar el vinculo recreado.
			ProductRequestDto desactivar = pedidoEdicion(categoria, atributo, varianteId, valor4);
			desactivar.setActive(false);
			assertThat(productServices.updateProduct(productoId, desactivar)).isTrue();
			entityManager.flush();
			entityManager.clear();

			Product guardado = productRepository.findById(productoId).orElseThrow();
			assertThat(guardado.getActive()).isFalse();
			assertThat(guardado.getVariants()).hasSize(1);
			assertThat(guardado.getVariants().get(0).getAttributes()).hasSize(1);

			// Cambiar el valor sólo reemplaza el vinculo afectado
			ProductRequestDto cambiarValor = pedidoEdicion(categoria, atributo, varianteId, valor8);
			assertThat(productServices.updateProduct(productoId, cambiarValor)).isTrue();
			entityManager.flush();
			entityManager.clear();

			Product actualizado = productRepository.findById(productoId).orElseThrow();
			assertThat(actualizado.getVariants().get(0).getAttributes()).hasSize(1);
			assertThat(actualizado.getVariants().get(0).getAttributes().get(0).getAttributeValue().getId())
					.isEqualTo(valor8);
		} finally {
			TenantContext.clear();
		}
	}

	private static ProductRequestDto pedidoEdicion(ProductType categoria, AttributeDto atributo, Long varianteId,
			Long valorId) {
		ProductRequestDto request = new ProductRequestDto();
		request.setName("Producto IT");
		request.setProductTypeId(categoria.getId());
		request.setAttributeIds(List.of(atributo.getId()));
		request.setVariants(List.of(variante(varianteId, valorId)));
		return request;
	}

	private static ProductVariantRequestDto variante(Long id, Long valorId) {
		ProductVariantRequestDto dto = new ProductVariantRequestDto();
		dto.setId(id);
		dto.setSku("IT-SKU-1");
		dto.setCost(new BigDecimal("100"));
		dto.setPrice(new BigDecimal("200"));
		dto.setStock(3);
		dto.setAttributeValueIds(List.of(valorId));
		return dto;
	}

}
