package org.vitrivr.cineast.core.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;


public final class CacheConfig {
    /**
     * Enumeration representing the cache policy. The cache policy is applied by the DataFactory classes.
     */
    public enum Policy {
        FORCE_DISK_CACHE, /* Cineast always caches to disk whenever a cachable object is requested. */
        AUTOMATIC, /* Cineast decides whether or not to use a cache in a particular instance based on the settings and a given heuristic. */
        AVOID_CACHE /* Cineast never caches to disk. */
    }

    /** The default upper threshold for caching of BLOBs, which is 5MB. */
    private final static int DEFAULT_UPPER_THRESHOLD = 5242880;

    /** The default upper threshold for caching of BLOBs, which is 128KB. */
    private final static int DEFAULT_LOWER_THRESHOLD = 131072;

    /** Cache policy that should be applied. */
    private final Policy cachingPolicy;

    /** The threshold in bytes above which a cached version of a data object will be created in any case, regardless of the policy. Used to apply the AVOID_CACHE cache policy. */
    private final int upperThreshold;

    /** The threshold in bytes above which a cached version of a data object will be created, given that the cache policy is AUTOMATIC. */
    private final int lowerThreshold;

    /** Location of the cache, which points to the folder that will hold all the cache files.*/
    private final Path cacheLocation;

    /**
     * @param cachePolicy     Caching Policy
     * @param cacheLocation   the file system location of the disk cache
     */
    @JsonCreator
    public CacheConfig(
            @JsonProperty(value = "upperThreshold") int upperThreshold,
            @JsonProperty(value = "lowerThreshold") int lowerThreshold,
            @JsonProperty(value = "cachePolicy") Policy cachePolicy,
            @JsonProperty(value = "cacheLocation") String cacheLocation) {

        /* Assign cache location. */
        if (cacheLocation != null) {
            final Path location = Paths.get(cacheLocation);
            if (Files.isWritable(location) && Files.isReadable(location) && Files.isDirectory(location)) {
                this.cacheLocation = location;
            } else {
                this.cacheLocation = Paths.get("./cache");
            }
        } else {
            this.cacheLocation = Paths.get("./cache");
        }

        /* Assign upper threshold. */
        if (upperThreshold > 0) {
            this.upperThreshold = upperThreshold;
        } else {
            this.upperThreshold = DEFAULT_UPPER_THRESHOLD;
        }

        /* Assign lower threshold. */
        if (lowerThreshold > 0) {
            this.lowerThreshold = lowerThreshold;
        } else {
            this.lowerThreshold = DEFAULT_LOWER_THRESHOLD;
        }

        /* Assign cache policy. */
        if (cachePolicy != null) {
            this.cachingPolicy = cachePolicy;
        } else {
            this.cachingPolicy = Policy.AUTOMATIC;
        }
    }

    /**
     * @return the caching policy
     */
    @JsonProperty
    public final Policy getCachingPolicy() {
        return this.cachingPolicy;
    }

    /**
     * @return the file system location of the cache
     */
    @JsonProperty
    public final Path getCacheLocation() {
        return this.cacheLocation;
    }

    /**
     * Getter for the upper threshold for file cache creation.
     *
     * @return The upper threshold for file cache creation.
     */
    @JsonProperty
    public int getUpperThreshold() {
        return upperThreshold;
    }

    /**
     * Getter for the lower threshold for file cache creation.
     *
     * @return The lower threshold for file cache creation.
     */
    @JsonProperty
    public int getLowerThreshold() {
        return lowerThreshold;
    }
}
