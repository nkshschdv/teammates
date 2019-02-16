package teammates.storage.entity;

import java.time.Instant;
import java.util.Date;

import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.translate.CreateContext;
import com.googlecode.objectify.impl.translate.LoadContext;
import com.googlecode.objectify.impl.translate.SaveContext;
import com.googlecode.objectify.impl.translate.SkipException;
import com.googlecode.objectify.impl.translate.TypeKey;
import com.googlecode.objectify.impl.translate.ValueTranslator;
import com.googlecode.objectify.impl.translate.ValueTranslatorFactory;

/**
 * Base class for all entities persisted to the Datastore.
 */
public abstract class BaseEntity { // NOPMD

    /**
     * Translates between `java.time.Instant` in entity class and `java.util.Date` in Google Cloud Datastore.
     *
     * <p>See <a href="https://github.com/objectify/objectify/blob/v5.2/src/main/java/com/googlecode/objectify/annotation/Translate.java">@Translate annotation</a>
     * and <a href="https://github.com/objectify/objectify/blob/v5.2/src/main/java/com/googlecode/objectify/impl/translate/TranslatorFactory.java">TranslatorFactory</a></p>
     */
    public static class InstantTranslatorFactory extends ValueTranslatorFactory<Instant, Date> {

        public InstantTranslatorFactory() {
            super(Instant.class);
        }

        @Override
        protected ValueTranslator<Instant, Date> createValueTranslator(TypeKey<Instant> tk, CreateContext ctx, Path path) {
            return new ValueTranslator<Instant, Date>(Date.class) {
                @Override
                protected Instant loadValue(Date value, LoadContext ctx, Path path) throws SkipException {
                    return value == null ? null : value.toInstant();
                }

                @Override
                protected Date saveValue(Instant value, boolean index, SaveContext ctx, Path path) throws SkipException {
                    return value == null ? null : Date.from(value);
                }
            };
        }
    }
}
