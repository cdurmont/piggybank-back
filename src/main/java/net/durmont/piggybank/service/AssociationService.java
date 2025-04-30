package net.durmont.piggybank.service;


import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.logging.Log;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import net.durmont.piggybank.model.Association;
import net.durmont.piggybank.model.Instance;
import org.apache.commons.beanutils.BeanUtils;

import jakarta.enterprise.context.ApplicationScoped;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@ApplicationScoped
public class AssociationService {

    public Uni<Association> create(Long instanceId, Association newAssociation) {
        if (newAssociation.instance == null)
            newAssociation.instance = new Instance();
        newAssociation.instance.id = instanceId;

        return Panache.withTransaction(newAssociation::persist);
    }

    public Uni<List<Association>> list(Long instanceId, Association filter, Sort sort, Page page) {
        return Association.list(instanceId, filter, sort, page);
    }

    public Uni<Association> update(Long instanceId, Long id, Association association) {
        association.id = id;
        if (association.instance == null)
            association.instance = new Instance();
        association.instance.id = instanceId;

        return Panache.withTransaction(association::update);
    }

    public Uni<Long> delete(Long instanceId, Long id) {
        return Panache.withTransaction( () -> Association.delete(instanceId, id) );
    }
}
