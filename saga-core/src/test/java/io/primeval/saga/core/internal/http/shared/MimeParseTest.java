package io.primeval.saga.core.internal.http.shared;

import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

public class MimeParseTest extends TestCase {
    public MimeParseTest(String name) {
        super(name);
    }

    public void testParseMediaRange() {
        assertEquals("('application', 'xml', {'q':'1',})", MimeParse
                .parseMediaRange("application/xml;q=1").toString());
        assertEquals("('application', 'xml', {'q':'1',})", MimeParse
                .parseMediaRange("application/xml").toString());
        assertEquals("('application', 'xml', {'q':'1',})", MimeParse
                .parseMediaRange("application/xml;q=").toString());
        assertEquals("('application', 'xml', {'q':'1',})", MimeParse
                .parseMediaRange("application/xml ; q=").toString());
        assertEquals("('application', 'xml', {'q':'1','b':'other',})",
                MimeParse.parseMediaRange("application/xml ; q=1;b=other")
                        .toString());
        assertEquals("('application', 'xml', {'q':'1','b':'other',})",
                MimeParse.parseMediaRange("application/xml ; q=2;b=other")
                        .toString());
        // Java URLConnection class sends an Accept header that includes a
        // single *
        assertEquals("('*', '*', {'q':'.2',})", MimeParse.parseMediaRange(
                " *; q=.2").toString());
    }

    public void testRFC2616Example() {
        String accept = "text/*;q=0.3, text/html;q=0.7, text/html;level=1, text/html;level=2;q=0.4, */*;q=0.5";

        assertEquals(1.0f, MimeParse.quality("text/html;level=1", accept));
        assertEquals(0.7f, MimeParse.quality("text/html", accept));
        assertEquals(0.3f, MimeParse.quality("text/plain", accept));
        assertEquals(0.5f, MimeParse.quality("image/jpeg", accept));
        assertEquals(0.4f, MimeParse.quality("text/html;level=2", accept));
        assertEquals(0.7f, MimeParse.quality("text/html;level=3", accept));
    }

    public void testBestMatch() {
        List<String> mimeTypesSupported = Arrays.asList(split(
                "application/xbel+xml,application/xml", ','));

        // direct match
        assertEquals(MimeParse.bestMatch(mimeTypesSupported,
                "application/xbel+xml"), "application/xbel+xml");

        // direct match with a q parameter
        assertEquals(MimeParse.bestMatch(mimeTypesSupported,
                "application/xbel+xml;q=1"), "application/xbel+xml");

        // direct match of our second choice with a q parameter
        assertEquals(MimeParse.bestMatch(mimeTypesSupported,
                "application/xml;q=1"), "application/xml");

        // match using a subtype wildcard
        assertEquals(MimeParse.bestMatch(mimeTypesSupported,
                "application/*;q=1"), "application/xml");

        // match using a type wildcard
        assertEquals(MimeParse.bestMatch(mimeTypesSupported, "*/*"),
                "application/xml");

        mimeTypesSupported = Arrays.asList(split(
                "application/xbel+xml,text/xml", ','));

        // match using a type versus a lower weighted subtype
        assertEquals(MimeParse.bestMatch(mimeTypesSupported,
                "text/*;q=0.5,*/*;q=0.1"), "text/xml");

        // fail to match anything
        assertEquals(MimeParse.bestMatch(mimeTypesSupported,
                "text/html,application/atom+xml; q=0.9"), "");

        // common AJAX scenario
        mimeTypesSupported = Arrays.asList(split(
                "application/json,text/html", ','));
        assertEquals(MimeParse.bestMatch(mimeTypesSupported,
                "application/json,text/javascript, */*"), "application/json");

        // verify fitness ordering
        assertEquals(MimeParse.bestMatch(mimeTypesSupported,
                "application/json,text/html;q=0.9"), "application/json");
    }

    public void testSupportWildcards() {
        List<String> mimeTypesSupported = Arrays.asList(split(
                "image/*,application/xml", ','));

        // match using a type wildcard
        assertEquals(MimeParse.bestMatch(mimeTypesSupported, "image/png"),
                "image/*");
        // match using a wildcard for both requested and supported
        assertEquals(MimeParse.bestMatch(mimeTypesSupported, "image/*"),
                "image/*");
    }

    private String[] split(String string, char c) {
        return string.split(String.valueOf(c));
    }
}