package com.exprivia.nest.cruud.utils;

/**
 * Raccolta di costanti per comporre gli endpoint REST dei controller.
 * Centralizza path comuni e segmenti ripetuti.
 */
public class Endpoints {

    //Common endpoints

    /** Costante slash. */
    public static final String SLASH = "/";

    /** Segmento per richiedere tutti gli elementi. */
    public static final String ALL = "/all";

    /** Segmento per operazioni di delete. */
    public static final String DELETE = "/delete";

    public static final String NAME = "/name";

    //Property endpoints

    /** Prefisso per le API property. */
    public static final String PROPERTY = "/property";

    /** Segmento per le API di filtro. */
    public static final String FILTER = "/filter";

    //Transformer endpoints

    /** Prefisso per le API transformer. */
    public static final String TRANSFORMER = "/transformer";

    /** Segmento CSV. */
    public static final String CSV = "/csv";

    /** Segmento upload. */
    public static final String UPLOAD = "/upload";

    //Extraction endpoints

    /** Prefisso per le API extraction. */
    public static final String EXTRACTION = "/extraction";

    //External endpoints

    /** Prefisso per le API esterne. */
    public static final String EXTERNAL = "/external";

    /** Segmento per integrazione OpenCruise. */
    public static final String OPEN_CRUISE = "/open-cruise";

}
