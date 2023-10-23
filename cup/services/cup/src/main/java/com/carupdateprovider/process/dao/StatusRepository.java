package com.carupdateprovider.process.dao;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.carupdateprovider.process.model.persistence.StatusEntity;

/*
 * Repository for accessing StatusEntities from the DB.
 * Only active in the cup-history and ext-request-proxy service.
 */
@ConditionalOnExpression("${application.history} or ${application.ext-request-proxy}")
@Repository
public interface StatusRepository extends JpaRepository<StatusEntity, UUID> {

    List<StatusEntity> findAllByRequestedBetween(Timestamp start, Timestamp end);

    StatusEntity findFirstBy(Sort sort);

    @Modifying
    @Query(
            value = ""
                    + "INSERT INTO public.status (uuid,vin, requested)"
                    + "VALUES (?1,?2,?3)"
                    + "ON CONFLICT (uuid)"
                    + "DO UPDATE SET requested = ?3", nativeQuery = true
    )
    void upsertRequested(@Param(value = "uuid") UUID uuid,
                         @Param(value = "vin") String vin,
                         @Param(value = "requested") Timestamp requested);

    @Modifying
    @Query(
            value = ""
                    + "INSERT INTO public.status (uuid,vin, triggered)"
                    + "VALUES (?1,?2,?3)"
                    + "ON CONFLICT (uuid)"
                    + "DO UPDATE SET triggered = ?3", nativeQuery = true
    )
    void upsertTriggered(@Param(value = "uuid") UUID uuid,
                         @Param(value = "vin") String vin,
                         @Param(value = "triggered") Timestamp triggered);

    @Modifying
    @Query(
            value = ""
                    + "INSERT INTO public.status (uuid,vin, fetched)"
                    + "VALUES (?1,?2,?3)"
                    + "ON CONFLICT (uuid)"
                    + "DO UPDATE SET fetched = ?3", nativeQuery = true
    )
    void upsertFetched(@Param(value = "uuid") UUID uuid,
                       @Param(value = "vin") String vin,
                       @Param(value = "fetched") Timestamp fetched);

    @Modifying
    @Query(
            value = ""
                    + "INSERT INTO public.status (uuid,vin, unfetchable)"
                    + "VALUES (?1,?2,?3)"
                    + "ON CONFLICT (uuid)"
                    + "DO UPDATE SET unfetchable = ?3", nativeQuery = true
    )
    void upsertUnfetchable(@Param(value = "uuid") UUID uuid,
                           @Param(value = "vin") String vin,
                           @Param(value = "unfetchable") Timestamp unfetchable);

    @Modifying
    @Query(
            value = ""
                    + "INSERT INTO public.status (uuid,vin, created)"
                    + "VALUES (?1,?2,?3)"
                    + "ON CONFLICT (uuid)"
                    + "DO UPDATE SET created = ?3", nativeQuery = true
    )
    void upsertCreated(@Param(value = "uuid") UUID uuid,
                       @Param(value = "vin") String vin,
                       @Param(value = "created") Timestamp created);

    @Modifying
    @Query(
            value = ""
                    + "INSERT INTO public.status (uuid,vin, rolled_out)"
                    + "VALUES (?1,?2,?3)"
                    + "ON CONFLICT (uuid)"
                    + "DO UPDATE SET rolled_out = ?3", nativeQuery = true
    )
    void upsertRolledOut(@Param(value = "uuid") UUID uuid,
                         @Param(value = "vin") String vin,
                         @Param(value = "rolled_out") Timestamp rolled_out);

    @Modifying
    @Query(
            value = ""
                    + "INSERT INTO public.status (uuid,vin, last_update)"
                    + "VALUES (?1,?2,?3)"
                    + "ON CONFLICT (uuid)"
                    + "DO UPDATE SET last_update = ?3", nativeQuery = true
    )
    void upsertLastUpdated(@Param(value = "uuid") UUID uuid,
                           @Param(value = "vin") String vin,
                           @Param(value = "last_updated") Timestamp last_updated);

}
