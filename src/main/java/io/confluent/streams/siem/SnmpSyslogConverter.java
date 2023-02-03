package io.confluent.streams.siem;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Converts SNMPv2 trap data into Syslog syslog (RFC 5424).
 * https://datatracker.ietf.org/doc/html/rfc5675
 * https://datatracker.ietf.org/doc/html/rfc5424
 */
public class SnmpSyslogConverter {

    public static JsonNode convert(JsonNode jsonNode) {
        //todo implement conversion
        return jsonNode;
    }

}