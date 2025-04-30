package net.durmont.piggybank.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import org.apache.commons.beanutils.BeanUtils;

import jakarta.persistence.Entity;
import java.lang.reflect.InvocationTargetException;

@Entity
public class Instance extends PanacheEntity {

	public String name;

	public Instance() {
	}

	public Instance(Long id) {
		this.id = id;
	}

	public static Instance valueOf(String json) {
		Instance instance = null;
		try {
			instance = new ObjectMapper().readValue(json, Instance.class);
		} catch (JsonProcessingException e) {
			Log.warn("Could not convert to Account from JSON : " + json, e);
		}

		return instance;
	}

	public Uni<Instance> update() {
		return Instance.<Instance>findById(id)
			.map(instanceDb -> {
				try {
					BeanUtils.copyProperties(instanceDb, this);
				} catch (IllegalAccessException | InvocationTargetException e) {
					Log.error("Error copying new properties of Instance " + id, e);
				}
				return instanceDb;
			});
	}
}
