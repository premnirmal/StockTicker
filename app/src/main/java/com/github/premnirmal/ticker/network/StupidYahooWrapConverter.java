package com.github.premnirmal.ticker.network;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import retrofit.converter.ConversionException;
import retrofit.mime.TypedInput;

/**
 * Created by premnirmal on 12/21/14.
 */
class StupidYahooWrapConverter extends BaseConverter {

    private static final Pattern PATTERN_RESPONSE = Pattern.compile("YAHOO\\.Finance\\.SymbolSuggest\\.ssCallback\\((\\{.*?\\})\\)");

    @Override
    public Object fromBody(TypedInput body, Type type) throws
            ConversionException {
        try {
            final String bodyString = getString(body.in());
            final Matcher m = PATTERN_RESPONSE.matcher(bodyString);
            if (m.find()) {
                final Suggestions suggestions = gson.fromJson(m.group(1), Suggestions.class);
                return suggestions;
            }
            throw new RuntimeException("Invalid response");
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
