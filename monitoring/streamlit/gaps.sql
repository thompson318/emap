SELECT *
FROM waveform
WHERE
            visit_observation_type_id = %s
  AND source_location = %s
ORDER BY observation_datetime
;
