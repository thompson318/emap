package uk.ac.ucl.rits.inform.datasinks.emapstar.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.ac.ucl.rits.inform.datasinks.emapstar.DataSources;
import uk.ac.ucl.rits.inform.datasinks.emapstar.RowState;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.HospitalVisitAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.HospitalVisitRepository;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisitAudit;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.informdb.movement.Location;
import uk.ac.ucl.rits.inform.interchange.adt.AdmissionDateTime;
import uk.ac.ucl.rits.inform.interchange.adt.AdtCancellation;
import uk.ac.ucl.rits.inform.interchange.adt.AdtMessage;
import uk.ac.ucl.rits.inform.interchange.adt.MoveVisitInformation;
import uk.ac.ucl.rits.inform.interchange.adt.RegisterPatient;
import uk.ac.ucl.rits.inform.interchange.adt.UpdateSubSpeciality;

import java.time.Instant;
import java.util.List;

/**
 * Interactions with visits.
 * @author Stef Piatek
 */
@Component
public class UpdateSubSpecialityController {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final HospitalVisitRepository hospitalVisitRepo;
    private final HospitalVisitAuditRepository hospitalVisitAuditRepo;

    /**
     * @param hospitalVisitRepo      repository for HospitalVisit
     * @param hospitalVisitAuditRepo repository for HospitalVisitAudit
     */
    public UpdateSubSpecialityController(HospitalVisitRepository hospitalVisitRepo, HospitalVisitAuditRepository hospitalVisitAuditRepo) {
        this.hospitalVisitRepo = hospitalVisitRepo;
        this.hospitalVisitAuditRepo = hospitalVisitAuditRepo;
    }

    /**
     * Update visit if message is from a trusted source update if newer or if database source isn't trusted.
     * Otherwise only update if if is newly created.
     * @param messageDateTime date time of the message
     * @param messageSource   Source system from the message
     * @param visitState      visit wrapped in state class
     * @return true if the visit should be updated
     */
    private boolean visitShouldBeUpdated(
            final Instant messageDateTime, final String messageSource, final RowState<HospitalVisit, HospitalVisitAudit> visitState) {
        // always update if a message is created
        if (visitState.isEntityCreated()) {
            return true;
        }
        HospitalVisit visit = visitState.getEntity();
        // if message source is trusted and (entity source system is untrusted or message is newer)
        return DataSources.isTrusted(messageSource)
                && (!DataSources.isTrusted(visit.getSourceSystem()) || !visit.getValidFrom().isAfter(messageDateTime));
    }

    /**
     * Update visit with generic ADT information.
     * @param msg        adt message
     * @param visitState visit wrapped in state class
     */
    private void updateGenericData(final AdtMessage msg, RowState<HospitalVisit, HospitalVisitAudit> visitState) {
        HospitalVisit visit = visitState.getEntity();
        visitState.assignInterchangeValue(msg.getPatientClass(), visit.getPatientClass(), visit::setPatientClass);
        visitState.assignInterchangeValue(msg.getModeOfArrival(), visit.getArrivalMethod(), visit::setArrivalMethod);
        visitState.assignIfDifferent(msg.getSourceSystem(), visit.getSourceSystem(), visit::setSourceSystem);
    }

    /**
     * Add admission date time.
     * @param msg        AdmissionDateTime
     * @param visitState visit wrapped in state class
     */
    private void addAdmissionDateTime(final AdmissionDateTime msg, RowState<HospitalVisit, HospitalVisitAudit> visitState) {
        HospitalVisit visit = visitState.getEntity();
        visitState.assignInterchangeValue(msg.getAdmissionDateTime(), visit.getAdmissionDatetime(), visit::setAdmissionDatetime);
    }

    /**
     * Delete admission specific information.
     * @param msg        cancellation message
     * @param visitState visit wrapped in state class
     */
    private void removeAdmissionInformation(final AdtCancellation msg, RowState<HospitalVisit, HospitalVisitAudit> visitState) {
        HospitalVisit visit = visitState.getEntity();
        visitState.removeIfExists(visit.getAdmissionDatetime(), visit::setAdmissionDatetime, msg.getCancelledDateTime());
    }

    /**
     * Add registration specific information.
     * @param msg        adt message
     * @param visitState visit wrapped in state class
     */
    private void addRegistrationInformation(final RegisterPatient msg, RowState<HospitalVisit, HospitalVisitAudit> visitState) {
        HospitalVisit visit = visitState.getEntity();
        visitState.assignInterchangeValue(msg.getPresentationDateTime(), visit.getPresentationDatetime(), visit::setPresentationDatetime);
    }

    /**
     * Add discharge specific information.
     * If no value for admission time, add this in from the discharge message.
     * @param msg        adt message
     * @param visitState visit wrapped in state class
     */
    private void addDischargeInformation(final DischargePatient msg, RowState<HospitalVisit, HospitalVisitAudit> visitState) {
        HospitalVisit visit = visitState.getEntity();
        visitState.assignIfDifferent(msg.getDischargeDateTime(), visit.getDischargeDatetime(), visit::setDischargeDatetime);
        visitState.assignIfDifferent(msg.getDischargeDisposition(), visit.getDischargeDisposition(), visit::setDischargeDisposition);
        visitState.assignIfDifferent(msg.getDischargeLocation(), visit.getDischargeDestination(), visit::setDischargeDestination);

        // If started mid-stream, no admission information so add this in on discharge
        if (visit.getAdmissionDatetime() == null && !msg.getAdmissionDateTime().isUnknown()) {
            visitState.assignInterchangeValue(msg.getAdmissionDateTime(), visit.getAdmissionDatetime(), visit::setAdmissionDatetime);
        }
    }

    /**
     * Remove discharge specific information.
     * @param msg        cancellation message
     * @param visitState visit wrapped in state class
     */
    private void removeDischargeInformation(final AdtCancellation msg, RowState<HospitalVisit, HospitalVisitAudit> visitState) {
        HospitalVisit visit = visitState.getEntity();
        visitState.removeIfExists(visit.getDischargeDatetime(), visit::setDischargeDatetime, msg.getCancelledDateTime());
        visitState.removeIfExists(visit.getDischargeDisposition(), visit::setDischargeDisposition, msg.getCancelledDateTime());
        visitState.removeIfExists(visit.getDischargeDestination(), visit::setDischargeDestination, msg.getCancelledDateTime());

    }


    public List<HospitalVisit> getOlderVisits(Mrn mrn, Instant messageDateTime) {
        return hospitalVisitRepo.findAllByMrnIdAndValidFromIsLessThanEqual(mrn, messageDateTime);
    }

    /**
     * @param msg MoveVisitInformation
     * @return true if the message visit number changes and the final encounter already exists
     */
    private boolean isVisitNumberChangesAndFinalEncounterAlreadyExists(MoveVisitInformation msg) {
        return !msg.getPreviousVisitNumber().equals(msg.getVisitNumber()) && hospitalVisitRepo.findByEncounter(msg.getVisitNumber()).isPresent();
    }

    /**
     * Deletes visit and updates corresponding audit table.
     * @param visit             List of hospital visits
     * @param invalidationTime  Time of the delete information message
     * @param deletionTime      time that emap-core started processing the message.
     */
    public void deleteVisit(HospitalVisit visit, Instant invalidationTime, Instant deletionTime) {
        hospitalVisitAuditRepo.save(visit.createAuditEntity(invalidationTime, deletionTime));
        hospitalVisitRepo.delete(visit);
    }

     /**
     * Process an Update subspeciality (Z99) request.
     * <p>
     * The Hl7 feed will eventually be changed so that we have an identifier per pending transfer, until then we guarantee the order of cancellations.
     * If we get messages out of order and have several cancellation messages before we receive any requests,
     * then the first request message for the location and encounter will add the eventDatetime to the earliest cancellation.
     * Subsequent requests will add the eventDatetime to the earliest cancellation with no eventDatetime, or create a new request if none exist
     * after the pending request eventDatetime.
     * @param visit      associated visit
     * @param msg        update sub speciality
     * @param validFrom  time in the hospital when the message was created
     * @param storedFrom time that emap core started processing the message
     */
    public void processMsg(HospitalVisit visit, UpdateSubSpeciality msg, Instant validFrom, Instant storedFrom) {
        Location plannedLocation = null;

    }


}
