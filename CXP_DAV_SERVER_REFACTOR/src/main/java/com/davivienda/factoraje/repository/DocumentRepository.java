package com.davivienda.factoraje.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.davivienda.factoraje.domain.entities.DocumentModel;
import com.davivienda.factoraje.domain.enums.DocumentStatusEnum;
import com.davivienda.factoraje.dto.document.DocumentHistorySummaryDTOResponse;

import jakarta.persistence.LockModeType;

@Repository
public interface DocumentRepository extends JpaRepository<DocumentModel, UUID>, JpaSpecificationExecutor<DocumentModel> {

        /** Los parámetros deben venir en mayúsculas; devuelve los valores en mayúsculas. */
        @Query("SELECT UPPER(d.generationCode) FROM DocumentModel d WHERE UPPER(d.generationCode) IN :generationCodes")
        Set<String> findExistingGenerationCodes(@Param("generationCodes") List<String> generationCodes);

        @Query("SELECT UPPER(d.controlNumber) FROM DocumentModel d WHERE UPPER(d.controlNumber) IN :controlNumbers")
        Set<String> findExistingControlNumbers(@Param("controlNumbers") List<String> controlNumbers);

        @Query("SELECT UPPER(d.receivedStamp) FROM DocumentModel d WHERE UPPER(d.receivedStamp) IN :receivedStamps")
        Set<String> findExistingReceivedStamps(@Param("receivedStamps") List<String> receivedStamps);

        /**
         * Documentos de un convenio en un estado dado cuya fecha de vencimiento es
         * posterior a {@code minDueDate} (exclusivo). Se usa para excluir los que
         * están por vencer.
         */
        @Query("""
                            SELECT d
                            FROM DocumentModel d
                            WHERE d.masterAgreement.id = :masterAgreementId
                            AND d.status = :status
                            AND d.dueDate > :minDueDate
                            ORDER BY d.issueDate ASC
                        """)
        List<DocumentModel> findByMasterAgreementAndStatusAndDueDateAfter(
                        @Param("masterAgreementId") UUID masterAgreementId,
                        @Param("status") DocumentStatusEnum status,
                        @Param("minDueDate") LocalDate minDueDate);

        @Query("SELECT d.documentNumber FROM DocumentModel d " +
                        "WHERE d.masterAgreement.supplier.nit = :nit " +
                        "AND d.documentNumber IN :documentNumbers " +
                        "AND EXTRACT(YEAR FROM d.issueDate) = :year")
        Set<String> findExistingPhysicalDocuments(
                        @Param("nit") String nit,
                        @Param("documentNumbers") List<String> documentNumbers,
                        @Param("year") int year);

        /** Bitácoras paginadas: trae convenio, pagador, proveedor y quién cargó en la misma consulta. */
        @Override
        @EntityGraph(attributePaths = { "masterAgreement", "masterAgreement.payer", "masterAgreement.supplier",
                        "uploadBatch", "uploadBatch.uploadedAndApprovedBy" })
        Page<DocumentModel> findAll(Specification<DocumentModel> spec, Pageable pageable);

        @Query("""
                            SELECT new com.davivienda.factoraje.dto.document.DocumentHistorySummaryDTOResponse(
                                s.id, s.name, s.nit, d.status, COUNT(d), COALESCE(SUM(d.nominalAmount), 0))
                            FROM DocumentModel d
                            JOIN d.masterAgreement ma
                            JOIN ma.supplier s
                            WHERE ma.payer.id = :payerId
                            GROUP BY s.id, s.name, s.nit, d.status
                            ORDER BY s.name
                        """)
        List<DocumentHistorySummaryDTOResponse> summarizeHistoryByPayer(@Param("payerId") UUID payerId);

        /**
         * Documentos de un convenio en un estado dado que vencen en o antes de
         * {@code maxDueDate} (inclusive): los que ya no pueden financiarse.
         * PostgreSQL vuelve a evaluar el estado tras esperar el bloqueo, así que una
         * transacción concurrente que ya los cambió hace que no se devuelvan.
         */
        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("""
                            SELECT d FROM DocumentModel d
                            WHERE d.masterAgreement.id = :masterAgreementId
                            AND d.status = :status
                            AND d.dueDate <= :maxDueDate
                            ORDER BY d.id
                        """)
        List<DocumentModel> findByMasterAgreementAndStatusAndDueDateLessThanEqualForUpdate(
                        @Param("masterAgreementId") UUID masterAgreementId,
                        @Param("status") DocumentStatusEnum status,
                        @Param("maxDueDate") LocalDate maxDueDate);

        /** Ordenados por id para que dos transacciones los bloqueen en el mismo orden. */
        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("SELECT d FROM DocumentModel d WHERE d.id IN :ids ORDER BY d.id")
        List<DocumentModel> findAllByIdForUpdate(@Param("ids") Collection<UUID> ids);

        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("SELECT d FROM DocumentModel d WHERE d.id = :id")
        Optional<DocumentModel> findByIdForUpdate(@Param("id") UUID id);

        /**
         * Posibles documentos a dispersar, sin lote: los que están en cuarentena y los
         * Cargados que vencen en o antes de {@code maxApprovedDueDate}. El llamador
         * descarta los Cargados que siguen siendo financiables bajo la política de su convenio.
         */
        @Query("""
                            SELECT d FROM DocumentModel d
                            JOIN FETCH d.masterAgreement ma
                            JOIN FETCH ma.payer p
                            JOIN FETCH ma.supplier
                            LEFT JOIN FETCH ma.disbursementPolicy
                            WHERE p.id = :payerId
                            AND d.dispersionBatch IS NULL
                            AND (d.status = :quarantined
                                 OR (d.status = :approved AND d.dueDate <= :maxApprovedDueDate))
                        """)
        List<DocumentModel> findDispersionCandidatesByPayer(
                        @Param("payerId") UUID payerId,
                        @Param("quarantined") DocumentStatusEnum quarantined,
                        @Param("approved") DocumentStatusEnum approved,
                        @Param("maxApprovedDueDate") LocalDate maxApprovedDueDate);

        /** Igual que {@link #findDispersionCandidatesByPayer} para todos los pagadores. */
        @Query("""
                            SELECT d FROM DocumentModel d
                            JOIN FETCH d.masterAgreement ma
                            JOIN FETCH ma.payer
                            LEFT JOIN FETCH ma.disbursementPolicy
                            WHERE d.dispersionBatch IS NULL
                            AND (d.status = :quarantined
                                 OR (d.status = :approved AND d.dueDate <= :maxApprovedDueDate))
                        """)
        List<DocumentModel> findDispersionCandidates(
                        @Param("quarantined") DocumentStatusEnum quarantined,
                        @Param("approved") DocumentStatusEnum approved,
                        @Param("maxApprovedDueDate") LocalDate maxApprovedDueDate);

        /**
         * Documentos sin lote de dispersión de un pagador, en los estados indicados y con el
         * vencimiento dado, bloqueados. Sin joins en el FROM para que el bloqueo solo
         * alcance a las filas de documentos.
         */
        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("""
                            SELECT d FROM DocumentModel d
                            WHERE d.masterAgreement.id IN (
                                SELECT ma.id FROM MasterAgreementModel ma WHERE ma.payer.id = :payerId)
                            AND d.dueDate = :dueDate
                            AND d.dispersionBatch IS NULL
                            AND d.status IN :statuses
                            ORDER BY d.id
                        """)
        List<DocumentModel> findWithoutDispersionBatchForUpdate(
                        @Param("payerId") UUID payerId,
                        @Param("dueDate") LocalDate dueDate,
                        @Param("statuses") Collection<DocumentStatusEnum> statuses);

        @Query("""
                            SELECT d FROM DocumentModel d
                            JOIN FETCH d.masterAgreement ma
                            JOIN FETCH ma.payer
                            JOIN FETCH ma.supplier s
                            LEFT JOIN FETCH d.uploadBatch ub
                            LEFT JOIN FETCH ub.uploadedAndApprovedBy
                            WHERE d.dispersionBatch.id = :batchId
                            ORDER BY s.name, d.dueDate, d.id
                        """)
        List<DocumentModel> findByDispersionBatchIdWithDetails(@Param("batchId") UUID batchId);

        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("SELECT d FROM DocumentModel d WHERE d.dispersionBatch.id = :batchId ORDER BY d.id")
        List<DocumentModel> findByDispersionBatchIdForUpdate(@Param("batchId") UUID batchId);

        /** Filas [batchId, cantidad de proveedores distintos]. */
        @Query("""
                            SELECT d.dispersionBatch.id, COUNT(DISTINCT d.masterAgreement.supplier.id)
                            FROM DocumentModel d
                            WHERE d.dispersionBatch.id IN :batchIds
                            GROUP BY d.dispersionBatch.id
                        """)
        List<Object[]> countSuppliersByDispersionBatch(@Param("batchIds") Collection<UUID> batchIds);
}
