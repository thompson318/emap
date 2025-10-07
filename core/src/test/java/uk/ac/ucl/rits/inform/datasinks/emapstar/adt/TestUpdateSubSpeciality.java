package uk.ac.ucl.rits.inform.datasinks.emapstar.adt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ucl.rits.inform.datasinks.emapstar.MessageProcessingBase;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.CoreDemographicRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.HospitalVisitRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.MrnRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.PlannedMovementRepository;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.movement.PlannedMovement;
import uk.ac.ucl.rits.inform.interchange.test.helpers.InterchangeMessageFactory;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;
import uk.ac.ucl.rits.inform.interchange.adt.AdtMessage;
import uk.ac.ucl.rits.inform.interchange.adt.CancelPendingTransfer;
import uk.ac.ucl.rits.inform.interchange.adt.UpdateSubSpeciality;
import uk.ac.ucl.rits.inform.interchange.adt.PendingType;

import java.io.IOException;
import java.io.InvalidClassException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class TestUpdateSubSpeciality extends MessageProcessingBase {
    private static final Logger logger = LoggerFactory.getLogger(TestPendingAdt.class);
    @Autowired
    private MrnRepository mrnRepository;
    @Autowired
    private CoreDemographicRepository coreDemographicRepository;
    @Autowired
    private HospitalVisitRepository hospitalVisitRepository;
    @Autowired
    private PlannedMovementRepository plannedMovementRepository;

    // end to end messages
    private UpdateSubSpeciality updateSubSpeciality;

    private static final String VISIT_NUMBER = "123412341234";
    private static final String LOCATION_STRING = "1020100166^SDEC BY02^11 SDEC";
    private static final Instant EVENT_TIME = Instant.parse("2022-04-21T23:22:58Z");
    private static final Instant CANCEL_TIME = Instant.parse("2022-04-21T23:37:58Z");


    private PlannedMovement getPlannedMovementOrThrow(String visitNumber, String location) {
        return plannedMovementRepository
                .findByHospitalVisitIdEncounterAndLocationIdLocationString(visitNumber, location).orElseThrow();
    }

    @BeforeEach
    void setup() throws IOException {
        updateSubSpeciality = messageFactory.getAdtMessage("Location/Moves/09_Z99.yaml");
    }


    /**
      If no entities exist in the database then latest service is NULL, 
      create an entry with the correct sub speciality/
    */
    @Test
    void testPendingCreatesOtherEntities() throws Exception {
        dbOps.processMessage(updateSubSpeciality);
        Optional<HospitalVisit> byId = hospitalVisitRepository.findByEncounter(VISIT_NUMBER);
        /*
         get the most recent planned movement - there shouldn't be one, so will throw a
         NoSuchElementException
        */
        assertThrows(NoSuchElementException.class, () -> getPlannedMovementOrThrow(VISIT_NUMBER, LOCATION_STRING));
    }


    /**
     * Given that no entities exist in the database
     * When a Z99 Message is created
     * Mrn, core demographics and hospital visit entities should be created
     * @throws Exception shouldn't happen
     */
    @Test
    void testUpdateCreatesOtherEntities() throws Exception {
        dbOps.processMessage(updateSubSpeciality);

        assertEquals(1, mrnRepository.count());
        assertEquals(1, coreDemographicRepository.count());
        assertEquals(1, hospitalVisitRepository.count());
    }

}

