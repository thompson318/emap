package uk.ac.ucl.rits.inform.datasinks.emapstar.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.MessageIgnoredException;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.LocationVisitRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.NotesMetadataRepository;
//import uk.ac.ucl.rits.inform.informdb.movement.LocationVisit;
//import uk.ac.ucl.rits.inform.informdb.visit_recordings.VisitObservationType;
//import uk.ac.ucl.rits.inform.informdb.notes.NotesMetadata;
//import uk.ac.ucl.rits.inform.interchange.InterchangeValue;
import uk.ac.ucl.rits.inform.interchange.NotesMetadataMessage;

import java.time.Instant;
//import java.util.List;
//import java.util.Optional;

/**
 * Controller for NotesMetadata specific information.
 * @author Sarah Keating
 */
@Component
public class NotesMetadataController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final NotesMetadataRepository notesMetadataRepository;
    private final LocationVisitRepository locationVisitRepository;

    NotesMetadataController(
            NotesMetadataRepository notesMetadataRepository,
            LocationVisitRepository locationVisitRepository
    ) {
        this.notesMetadataRepository = notesMetadataRepository;
        this.locationVisitRepository = locationVisitRepository;
    }

    /**
     * Process notesMetadata data message.
     * @param msg the interchange message
     * @param storedFrom stored from timestamp
     * @throws MessageIgnoredException if message not processed
     */
    @Transactional
    public void processMessage(
            NotesMetadataMessage msg,
            Instant storedFrom) throws MessageIgnoredException {
//        InterchangeValue<List<Double>> interchangeValue = msg.getNumericValues();
//        if (!interchangeValue.isSave()) {
//            throw new MessageIgnoredException("Updating/deleting notesMetadata data is not supported");
//        }
//        // All given values are put into one new row. It's the responsibility of whoever is
//        // generating the message to choose an appropriate size of array.
//        List<Double> numericValues = interchangeValue.get();
//        Instant observationTime = msg.getObservationTime();
//        // Try to find the visit. We don't have enough information to create the visit if it doesn't already exist.
//        Optional<LocationVisit> inferredLocationVisit =
//                locationVisitRepository.findLocationVisitByLocationAndTime(observationTime, msg.getMappedLocationString());
//        // XXX: will have to do some sanity checks here to be sure that the HL7 feed hasn't gone down.
//        // See issue #36, and here for discussion:
//        // https://github.com/SAFEHR-data/emap/blob/develop/docs/dev/features/notesMetadata_hf_data.md#core-processor-logic-orphan-data-problem
//        NotesMetadata dataRow = new NotesMetadata(
//                observationTime,
//                observationTime,
//                storedFrom);
//        inferredLocationVisit.ifPresent(dataRow::setLocationVisitId);
//        Double[] valuesAsArray = numericValues.toArray(new Double[0]);
//        dataRow.setSamplingRate(msg.getSamplingRate());
//        dataRow.setSourceLocation(msg.getSourceLocationString());
//        dataRow.setVisitObservationTypeId(visitObservationType);
//        dataRow.setUnit(msg.getUnit());
//        dataRow.setValuesArray(valuesAsArray);
//        notesMetadataRepository.save(dataRow);
    }

//    /**
//     * Delete notesMetadata data before the cutoff date.
//     * @param olderThanCutoff cutoff date
//     * @return number of rows deleted
//     */
//    @Transactional
//    public int deleteOldWaveformData(Instant olderThanCutoff) {
//        return notesMetadataRepository.deleteAllInBatchByObservationDatetimeBefore(olderThanCutoff);
//    }
//
//    /**
//     * @return Return observation datetime of most recent notesMetadata data.
//     */
//    public Instant mostRecentObservationDatatime() {
//        return notesMetadataRepository.mostRecentObservationDatatime();
  //  }
}
