package com.exprivia.nest.cruud.utils;

import com.exprivia.nest.cruud.dto.ExtractionDto;
import com.exprivia.nest.cruud.dto.sourcedataset.ValueDto;
import com.exprivia.nest.cruud.dto.urbandataset.context.ContextDto;
import com.exprivia.nest.cruud.dto.urbandataset.specification.IdDto;
import com.exprivia.nest.cruud.dto.urbandataset.specification.SpecificationDto;
import com.exprivia.nest.cruud.dto.urbandataset.values.PropertyValueDto;
import com.exprivia.nest.cruud.utils.TimeUtils;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Mapping helpers for CSV values and extraction metadata.
 */
public final class MappingUtils {

    private MappingUtils() {
    }

    /**
     * Update specification and context for UD based on extraction object.
     *
     * @param specificationDto to update
     * @param contextDto to update
     * @param extractionDto parametrized
     */
    public static void updateSpecificationAndContext(SpecificationDto specificationDto, ContextDto contextDto, ExtractionDto extractionDto) {
        if (extractionDto.getProducerID() != null) {
            specificationDto.setId(
                    IdDto.builder()
                            .value(extractionDto.getProducerID())
                            .schemeID(specificationDto.getId().getSchemeID())
                            .build()
            );
        } else if (specificationDto != null) {
            String[] pathSplitted = extractionDto.getSourceFilesPath().split("\\\\|/");

            specificationDto.setId(
                    IdDto.builder()
                            .value(pathSplitted[pathSplitted.length - 1])
                            .schemeID(specificationDto.getId().getSchemeID())
                            .build()
            );
        }


        if (extractionDto.getCoordinates() != null) {
            contextDto.setCoordinates(extractionDto.getCoordinates());
        }

        if (extractionDto.getUdUtc() != null && !extractionDto.getUdUtc().isBlank()) {
            String label = TimeUtils.formatUtcOffsetLabel(extractionDto.getUdUtc());
            contextDto.setTimeZone(label != null ? label : extractionDto.getUdUtc());
        }
    }

    /**
     * Method that add null fields into properties.
     *
     * @param list propertyValueList
     * @param mapNulls map of nulls
     */
    public static void addNullFields(List<PropertyValueDto> list, Map<String, String> mapNulls) {
        if (mapNulls == null || mapNulls.isEmpty()) {
            return;
        }
        mapNulls.forEach((key, value) -> {
            if (list.stream().noneMatch(obj -> obj.getName().equalsIgnoreCase(key))) {
                list.add(PropertyValueDto.builder().name(key).val(value).build());
            }
        });
    }

    /**
     * Safely convert an arbitrary object into a {@code Map<String, String>} representation.
     * Non-map inputs result in an empty map. Keys and values are stringified; a null value
     * is converted to the literal string "null".
     *
     * @param candidate object to convert (expected Map-like)
     * @return mutable map of string keys and values
     */
    public static Map<String, String> toStringMap(Object candidate) {
        Map<String, String> result = new HashMap<>();
        if (candidate instanceof Map<?, ?> source) {
            source.forEach((key, value) -> result.put(
                    key != null ? key.toString() : null,
                    value != null ? value.toString() : "null"
            ));
        }
        return result;
    }

    /**
     * Method to add new value into property list.
     *
     * @param property to find
     * @param header of csv
     * @param line value from csv
     * @param propertiesValue list properties value
     * @param dictionary dictionary from getMapping
     */
    public static void addValueToPropertyList(String property,
                HashMap<String, LinkedList<Integer>> finalHeaderList, int findValue, HashMap<String, String> line,
                List<PropertyValueDto> propertiesValue, List<ValueDto> dictionary) {

        String[] header = finalHeaderList.keySet().toArray(new String[0]);

        for (int k = 0; k < header.length; k++) {
            var columnName = header[k];

            if (columnName.equalsIgnoreCase(property)) {
                String raw = line.get(columnName);
                if (raw == null) {
                    return;
                }
                propertiesValue.add(PropertyValueDto.builder()
                        .name(columnName)
                        .val(getCorrectValue(columnName, raw, dictionary))
                        .build());
            }
        }

    }

    /**
     * Method to get a correct value after elaboration.
     *
     * @param header from csv
     * @param value from csv
     * @param dictionary value from mapping
     * @return string value
     */
    public static String getCorrectValue(String header, String value, List<ValueDto> dictionary) {

        String sanitized = normalizeDecimalSeparator(value);

        if (sanitized == null || sanitized.isBlank()) {
            return "0";
        }

        for (ValueDto valueDto : dictionary) {
            if (valueDto.getName().equalsIgnoreCase(header) ||
                    (valueDto.getNameForNegative() != null && valueDto.getNameForNegative().equalsIgnoreCase(header))) {

                String result = (valueDto.getFunction() == null || valueDto.getFunction().isBlank())
                        ? sanitized
                        : Maths.execute(sanitized, valueDto.getFunction(), valueDto.getAlternativeValue());

                return result != null ? result : "0";
            }
        }

        return sanitized;

    }

    /**
     * Method to get number or null string value.
     *
     * @param value param to evaluate
     * @return correct string value
     */
    public static String getNumberOrNullString(String value, String alternativeResponse) {
        try {
            String normalized = normalizeDecimalSeparator(value);
            Double.parseDouble(normalized);
            return normalized;
        } catch (NumberFormatException e) {
            return alternativeResponse;
        }
    }

    /**
     * Method to get positive number or null string value.
     *
     * @param value param to evaluate
     * @param alternativeResponse alternative response if param doesn't respect
     * @return correct string value
     */
    public static String getPositiveOrNullString(String value, String alternativeResponse) {
        try {
            String normalized = normalizeDecimalSeparator(value);
            return Double.parseDouble(normalized) >= 0 ? normalized : alternativeResponse;
        } catch (NumberFormatException e) {
            return alternativeResponse;
        }
    }

    /**
     * Normalize decimal separator converting commas to dots.
     */
    public static String normalizeDecimalSeparator(String value) {
        if (value == null) {
            return null;
        }
        return value.replace(',', '.').trim();
    }
}
