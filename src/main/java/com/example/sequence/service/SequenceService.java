package com.example.sequence.service;

import com.example.sequence.model.HistoryRecord;
import com.example.sequence.model.Sequence;
import com.example.sequence.repositories.HistoryRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Business logic for the Sequence API.
 *
 * Persistence:
 *  - Every method that reads or writes history goes through
 *    {@link HistoryRecordRepository}, which Spring Data JPA implements
 *    against Oracle XE at runtime.
 *  - Write methods are annotated @Transactional so Hibernate flushes the
 *    changes in a single transaction.
 *
 * Encoding (z-chain):
 *  - A "z-chain" is zero or more 'z' characters (each worth 26) followed by ONE
 *    terminator character (a=1, b=2, ..., z=26, '_'=0, anything else=0).
 *  - The first z-chain in the input is the HEADER and tells us how many values
 *    are in the package. Each value that follows is itself a z-chain.
 *  - After one package is read we start over with a new header. We repeat until
 *    the input is exhausted.
 */
@Service
public class SequenceService {

    private static final Logger log = LoggerFactory.getLogger(SequenceService.class);

    private final HistoryRecordRepository repo;

    public SequenceService(HistoryRecordRepository repo) {
        this.repo = repo;
    }

    /**
     * Process the raw input: validate, parse, and persist a HistoryRecord.
     *
     * @param input            the raw sequence string from the client
     * @param sourceIpAddress  the IP address of the HTTP caller
     * @return the saved HistoryRecord (with the database-assigned id)
     */
    @Transactional
    public HistoryRecord process(String input, String sourceIpAddress) {
        log.debug("Processing sequence input='{}' from {}", input, sourceIpAddress);
        if (!Sequence.isValid(input)) {
            log.warn("Rejecting invalid input '{}'", input);
            throw new IllegalArgumentException(
                "input must be non-empty and contain only letters and underscores");
        }
        List<Integer> output = parse(input.toLowerCase());
        HistoryRecord record = new HistoryRecord(
            null,
            LocalDateTime.now(),
            sourceIpAddress,
            input,
            output
        );
        HistoryRecord saved = repo.save(record);
        log.info("Saved history record id={} input='{}' output={}",
            saved.getId(), saved.getInput(), saved.getOutput());
        return saved;
    }

    /** Return all history records. */
    public List<HistoryRecord> getAllHistory() {
        List<HistoryRecord> all = repo.findAll();
        log.debug("Fetched {} history record(s)", all.size());
        return all;
    }

    /** Remove every history record from the database. */
    @Transactional
    public void clearHistory() {
        log.info("Clearing all history records");
        repo.deleteAll();
    }

    /** Return one history record by id. */
    public Optional<HistoryRecord> getHistoryById(Long id) {
        log.debug("Looking up history record id={}", id);
        return repo.findById(id);
    }

    /**
     * Update an existing history record by id.
     *
     * If the caller supplies a new input, we re-parse it to keep the output
     * consistent with the input (unless the caller also supplied an explicit
     * output, which then wins). The id and timestamp are never changed.
     */
    @Transactional
    public Optional<HistoryRecord> updateHistory(Long id,
                                                 String newInput,
                                                 List<Integer> newOutput,
                                                 String newSourceIpAddress) {
        log.debug("Updating history id={} newInput='{}' newOutput={} newIp='{}'",
            id, newInput, newOutput, newSourceIpAddress);

        Optional<HistoryRecord> found = repo.findById(id);
        if (found.isEmpty()) {
            log.warn("History record not found for update: id={}", id);
            return Optional.empty();
        }

        List<Integer> outputToStore = newOutput;
        if (newInput != null) {
            if (!Sequence.isValid(newInput)) {
                log.warn("Rejecting invalid input on update id={}: '{}'", id, newInput);
                throw new IllegalArgumentException(
                    "input must be non-empty and contain only letters and underscores");
            }
            if (outputToStore == null) {
                outputToStore = parse(newInput.toLowerCase());
            }
        }

        HistoryRecord r = found.get();
        if (newInput != null) {
            r.setInput(newInput);
        }
        if (outputToStore != null) {
            r.setOutput(outputToStore);
        }
        if (newSourceIpAddress != null) {
            r.setSourceIpAddress(newSourceIpAddress);
        }
        HistoryRecord saved = repo.save(r);
        log.info("History record updated: id={}", id);
        return Optional.of(saved);
    }

    // ---------------- parsing helpers ----------------

    private List<Integer> parse(String s) {
        List<Integer> packages = new ArrayList<>();
        int[] cursor = {0};
        int n = s.length();
        while (cursor[0] < n) {
            int packageSize = readZChain(s, cursor);
            int sum = 0;
            for (int v = 0; v < packageSize && cursor[0] < n; v++) {
                sum += readZChain(s, cursor);
            }
            packages.add(sum);
        }
        return packages;
    }

    private int readZChain(String s, int[] cursor) {
        int n = s.length();
        int value = 0;
        while (cursor[0] < n && s.charAt(cursor[0]) == 'z') {
            value += 26;
            cursor[0]++;
        }
        if (cursor[0] < n) {
            char c = s.charAt(cursor[0]);
            if (c >= 'a' && c <= 'z') {
                value += c - 'a' + 1;
            }
            cursor[0]++;
        }
        return value;
    }
}
