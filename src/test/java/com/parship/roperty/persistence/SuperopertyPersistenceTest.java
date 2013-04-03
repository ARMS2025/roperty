package com.parship.roperty.persistence;

import com.parship.roperty.Resolver;
import com.parship.roperty.Roperty;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.SQLException;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 * @author mfinsterwalder
 * @since 2013-04-02 15:15
 */
public class SuperopertyPersistenceTest {

	private static final String URL = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL";
	private static final String USER = "parship";
	private static final String PASSWORD = "freiheit";

	private Roperty roperty = new Roperty();
	private SuperopertyPersistence persistence;

	private static final String CREATE_BASE_TABLE = "CREATE TABLE base_property ( " +
		"id bigint, " +
		"property_name character varying(255) NOT NULL, " +
		"converter_class character varying(255), " +
		"converter_config character varying(255), " +
		"description character varying(1000), " +
		"default_value text, " +
		"inheritance_type character varying(255), " +
		"container_name character varying(255) NOT NULL, " +
		"last_changed timestamp, " +
		"change_user character varying(255), " +
		"version bigint DEFAULT 0 NOT NULL, " +
		"app_version character varying(30), " +
		"ctime timestamp DEFAULT now())";

	private static final String CREATE_DOMAIN_TABLE = "CREATE TABLE domain_property ( " +
		"id bigint, " +
		"base_property bigint NOT NULL, " +
		"domain character varying(255) NOT NULL, " +
		"overridden_value text, " +
		"last_changed timestamp, " +
		"change_user character varying(255), " +
		"version bigint DEFAULT 0 NOT NULL, " +
		"app_version character varying(30))";

	private static Persistence PERSISTENCE;

	@BeforeClass
	public static void beforeClass() throws SQLException {
		PERSISTENCE = getPersistence();
	}

	@Before
	public void before() {
		persistence = new SuperopertyPersistence(roperty, PERSISTENCE);
	}

	@Test
	public void basePropertiesAreRead() throws SQLException {
		PERSISTENCE.executeSql("INSERT INTO base_property (property_name, container_name, default_value) VALUES ('key', 'container', 'value')");
		this.persistence.loadAll();
		assertThat((String)roperty.get("key"), is("value"));
	}

	@Test
	public void domainPropertiesAreRead() throws SQLException {
		PERSISTENCE.executeSql("INSERT INTO base_property (id, property_name, container_name, default_value) VALUES (1, 'key', 'container', 'value')");
		PERSISTENCE.executeSql("INSERT INTO domain_property (base_property, domain, overridden_value) VALUES (1, 'LOCALE_de_DE', 'overridden')");
		this.persistence.loadAll();
		Resolver resolverMock = mock(Resolver.class);
		roperty.addDomain("container").addDomain("country").addDomain("locale");
		when(resolverMock.getDomainValue("container")).thenReturn("container");
		when(resolverMock.getDomainValue("country")).thenReturn("DE");
		when(resolverMock.getDomainValue("locale")).thenReturn("de_DE");
		roperty.setResolver(resolverMock);
		assertThat((String)roperty.get("key"), is("overridden"));
	}

	private static Persistence getPersistence() throws SQLException {
		Persistence persistence = new Persistence(URL, USER, PASSWORD);
		persistence.executeSql(CREATE_BASE_TABLE);
		persistence.executeSql(CREATE_DOMAIN_TABLE);
		return persistence;
	}

	@Test
	public void stripPrefix() {
		assertThat(persistence.stripPrefix("COUNTRY_DE"), is("DE"));
	}

	@Test
	public void suffix() {
		assertThat(persistence.suffix("LOCALE_de_DE"), is("DE"));
	}

	@Test
	public void prefix() {
		assertThat(persistence.prefix("LOCALE_de_DE"), is("LOCALE"));
	}

	@Test
	public void countryDomain() {
		assertThat(persistence.buildDomainKey("container", "COUNTRY_DE"), is(new String[]{"container", "DE"}));
	}

	@Test
	public void localeDomain() {
		assertThat(persistence.buildDomainKey("container", "LOCALE_de_DE"), is(new String[]{"container", "DE", "de_DE"}));
	}

	@Test
	public void orientationDomain() {
		assertThat(persistence.buildDomainKey("container", "ORIENTATION_GAY_es_MX"), is(new String[]{"container", "MX", "es_MX", "GAY"}));
	}

	@Test
	public void partnerDomain() {
		assertThat(persistence.buildDomainKey("container", "PARTNER_103_de_AT"), is(new String[]{"container", "AT", "de_AT", "*", "103"}));
	}
}
