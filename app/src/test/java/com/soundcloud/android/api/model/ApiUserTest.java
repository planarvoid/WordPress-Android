package com.soundcloud.android.api.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.json.JacksonJsonTransformer;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.users.UserItem;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.reflect.TypeToken;
import org.junit.Test;

import java.io.IOException;

public class ApiUserTest extends AndroidUnitTest {

    @Test
    public void shouldDefineEqualityBasedOnUrn() {
        ApiUser apiUser1 = ModelFixtures.create(ApiUser.class);
        ApiUser apiUser2 = ModelFixtures.create(ApiUser.class);
        apiUser2.setUrn(apiUser1.getUrn());

        assertThat(apiUser1).isEqualTo(apiUser2);
    }

    @Test
    public void shouldDefineHashCodeBasedOnUrn() {
        ApiUser apiUser1 = ModelFixtures.create(ApiUser.class);
        ApiUser apiUser2 = ModelFixtures.create(ApiUser.class);
        apiUser2.setUrn(apiUser1.getUrn());

        assertThat(apiUser1.hashCode()).isEqualTo(apiUser2.hashCode());
    }

    @Test
    public void shouldConvertToUserItem() {
        ApiUser apiUser = ModelFixtures.create(ApiUser.class);

        UserItem userItem = UserItem.from(apiUser);
        assertThat(userItem.getUrn()).isEqualTo(apiUser.getUrn());
        assertThat(userItem.name()).isEqualTo(apiUser.getUsername());
        assertThat(userItem.country().get()).isEqualTo(apiUser.getCountry());
        assertThat(userItem.followersCount()).isEqualTo(apiUser.getFollowersCount());
    }

    // The below exists purely to aid developers in completing stories regarding updating the profile.
    // I would suggest removing everything below here once we complete :
    // https://soundcloud.atlassian.net/browse/CREATORS-2240
    @Test
    public void convertSocialLinksToLegacy() throws IOException, ApiMapperException {
        ApiUser apiUser = new JacksonJsonTransformer().fromJson(json, new TypeToken<ApiUser>(){});

        assertThat(apiUser.getMyspaceName()).isEqualTo(Optional.of("dkbrothers"));
        assertThat(apiUser.getDiscogsName()).isEqualTo(Optional.of("DK+Brothers"));
        assertThat(apiUser.getWebsiteName()).isEqualTo(Optional.of("ASTROFONIK"));
        assertThat(apiUser.getWebsiteUrl()).isEqualTo(Optional.of("http://astrofonik.com/V2/index.php?keyword=dk%20brothers&Search=Buscar&Itemid=80&option=com_virtuemart&page=shop.browse"));
    }

    private final String json = "{\n" +
            "      \"urn\": \"soundcloud:users:887221\",\n" +
            "      \"permalink\": \"dk-brothers\",\n" +
            "      \"username\": \"DK BROTHERS\",\n" +
            "      \"first_name\": \"Breakstorm / Astrofonik / Teklicit Marsatek / TrivialTek / Synthax Error .\",\n" +
            "      \"last_name\": \"www.myspace.com/dkbrothers\",\n" +
            "      \"avatar_url\": \"https://i1.sndcdn.com/avatars-000276646158-p705v3-large.jpg\",\n" +
            "      \"city\": \"Barcelona\",\n" +
            "      \"country\": \"Spain\",\n" +
            "      \"country_code\": \"ES\",\n" +
            "      \"tracks_count\": 28,\n" +
            "      \"followers_count\": 6302,\n" +
            "      \"followings_count\": 3,\n" +
            "      \"verified\": false,\n" +
            "      \"is_pro\": false,\n" +
            "      \"description\": \"/////////////////////////////////////////\\r\\n\\r\\nBREAKSTORM / ASTROFONIK\\r\\nTEKLICIT MARSATEK & FSL prod\\r\\n\\r\\n*BOOK DATES NOW: info@breakstorm.com\\r\\n\\r\\n////////////////////////////////////////\\r\\n\\r\\n\\r\\n**NEXT EVENTS**\\r\\n\\r\\n\\r\\n-25/1 @ Xcore Festival /TARRAGONA\\r\\n\\r\\n-1/2 @ MAMA MANDAWA CLUB /CERDANYOLA\\r\\n\\r\\n-9/2 @ BLAU CLUB /GIRONA\\r\\n\\r\\n-15/2 T.B.C @ PALAFRUGELL AREA!\\r\\n\\r\\n-16/2 @ BREAKSTORM FESTIVAL /CERDANYOLA\\r\\n\\r\\n-9/3 @ SANDY WAREZ B-DAY! /BELGICA\\r\\n\\r\\n-30/4 @ BLAU CLUB /GIRONA\\r\\n\\r\\n\\r\\n\\r\\n---Discography---\\r\\n\\r\\n\\r\\n***SYNTHAX ERROR REC 01 (TEKNO DEALER E.P)*\\r\\n<a href=\\\"http://www.undergroundtekno.com/synthax-error-p-4633.html?language=en\\\" rel=\\\"nofollow\\\" target=\\\"_blank\\\">http://www.undergroundtekno.com/synthax-error-p-4633.html?language=en</a>\\r\\n\\r\\n\\r\\n***TRIVIALTEK REC 03*\\r\\n<a href=\\\"http://www.somixx.com/en/eps/profile/TrivialTek3+:+Ralla+que+me+meto,+bocadillo+que+me+ahorro\\\" rel=\\\"nofollow\\\" target=\\\"_blank\\\">http://www.somixx.com/en/eps/profile/TrivialTek3+:+Ralla+que+me+meto,+bocadillo+que+me+ahorro</a>\\r\\n\\r\\n\\r\\n***ASTROFONIK REC -EUROTEK 08\\r\\n<a href=\\\"http://astrofonik.com/V2/index.php?page=shop.product_details&amp;flypage=shop.flypage&amp;product_id=7088&amp;category_id=26&amp;manufacturer_id=0&amp;option=com_virtuemart&amp;Itemid=80\\\" rel=\\\"nofollow\\\" target=\\\"_blank\\\">http://astrofonik.com/V2/index.php?page=shop.product_details&amp;flypage=shop.flypage&amp;product_id=7088&amp;category_id=26&amp;manufacturer_id=0&amp;option=com_virtuemart&amp;Itemid=80</a>\\r\\n\\r\\n\\r\\n***ASTROFONIK REC on PARA-NOIZE 05!\\r\\n<a href=\\\"http://astrofonik.com/V2/index.php?page=shop.product_details&amp;category_id=26&amp;flypage=shop.flypage&amp;product_id=7463&amp;option=com_virtuemart&amp;Itemid=80&amp;vmcchk=1\\\" rel=\\\"nofollow\\\" target=\\\"_blank\\\">http://astrofonik.com/V2/index.php?page=shop.product_details&amp;category_id=26&amp;flypage=shop.flypage&amp;product_id=7463&amp;option=com_virtuemart&amp;Itemid=80&amp;vmcchk=1</a>\\r\\n\\r\\n\\r\\n*** CHIEN DE LA CASSE 12!\\r\\n<a href=\\\"http://www.undergroundtekno.com/chien-casse-p-5694.html\\\" rel=\\\"nofollow\\\" target=\\\"_blank\\\">http://www.undergroundtekno.com/chien-casse-p-5694.html</a>\\r\\n\\r\\n\\r\\n*** HISTERIA REC on REAKTOR SERIES 01\\r\\n<a href=\\\"http://www.somixx.com/en/eps/profile/REAKTOR+SERIES+01\\\" rel=\\\"nofollow\\\" target=\\\"_blank\\\">http://www.somixx.com/en/eps/profile/REAKTOR+SERIES+01</a>\\r\\n\\r\\n\\r\\n*** ASTROFONIK REC -PARA-NOIZE 07- // all tracks by DK BROTHERS!\\r\\nhttp://astrofonik.com/V2/index.php?page=shop.product_details&flypage=shop.flypage&product_id=8243&category_id=26&manufacturer_id=0&option=com_virtuemart&Itemid=80\\r\\n\\r\\n\\r\\n***ASTROFONIK REC -EUROTEK 09 (SEGUNDA PARTE)\\r\\nhttp://astrofonik.com/V2/index.php?page=shop.product_details&flypage=shop.flypage&product_id=8543&category_id=179&manufacturer_id=0&option=com_virtuemart&Itemid=80&vmcchk=1&Itemid=80\\r\\n\\r\\n\\r\\n***CHIEN DE LA CASSE 13! (FREE STYLE LISTEN prod)\\r\\nhttp://www.toolboxrecords.com/en/product/15498/tribe/chien-de-la-casse-13/\\r\\n\\r\\n***ASTROFONIK REC - PARA-NOIZE 09\\r\\n http://astrofonik.com/V2/index.php?page=shop.product_details&flypage=shop.flypage&product_id=8864&category_id=8&manufacturer_id=0&option=com_virtuemart&Itemid=80\\r\\n\\r\\n\\r\\n\\r\\n\\r\\n\\r\\n!!!!THANKS TO ALL FOR THE SUPORT!!!!!!\\r\\n\\r\\n\\r\\n***FOR CONTACT BOOKING:\\r\\n-<a href=\\\"mailto:info@breakstorm.com\\\" rel=\\\"nofollow\\\" target=\\\"_blank\\\">info@breakstorm.com</a>\\r\\n-<a href=\\\"mailto:dkbsound@gmail.com\\\" rel=\\\"nofollow\\\" target=\\\"_blank\\\">dkbsound@gmail.com</a>\\r\\n\\r\\n\",\n" +
            "      \"social_media_links\": {\n" +
            "        \"collection\": [\n" +
            "          {\n" +
            "            \"title\": \"website\",\n" +
            "            \"network\": \"personal\",\n" +
            "            \"url\": \"http://www.iwipa.com/iwipa/212308415473772?pid=1\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"title\": \"\",\n" +
            "            \"network\": \"facebook\",\n" +
            "            \"url\": \"http://facebook.com/profile.php?id=100000292672309\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"title\": \"\",\n" +
            "            \"network\": \"twitter\",\n" +
            "            \"url\": \"http://twitter.com/#!/DKBrothers\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"title\": \"\",\n" +
            "            \"network\": \"myspace\",\n" +
            "            \"url\": \"http://www.myspace.com/dkbrothers\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"title\": \"ASTROFONIK\",\n" +
            "            \"network\": \"personal\",\n" +
            "            \"url\": \"http://astrofonik.com/V2/index.php?keyword=dk%20brothers&Search=Buscar&Itemid=80&option=com_virtuemart&page=shop.browse\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"title\": \"Facebook Fan Page\",\n" +
            "            \"network\": \"facebook\",\n" +
            "            \"url\": \"https://www.facebook.com/pages/DK-BROTHERS-Astrofonik-Teklicit-Marsatek-Breakstorm/212308415473772\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"title\": \"BUY tracks on SOMIXX\",\n" +
            "            \"network\": \"other\",\n" +
            "            \"url\": \"http://www.somixx.com/en/artists/profile/DK%20BROTHERS\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"title\": \"BUY tracks on BEATPORT\",\n" +
            "            \"network\": \"beatport\",\n" +
            "            \"url\": \"http://www.beatport.com/artist/dk-brothers/204285\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"title\": \"BUY tracks on UNDERGROUNDTEKNO\",\n" +
            "            \"network\": \"other\",\n" +
            "            \"url\": \"http://www.undergroundtekno.com/advanced_search_result.php?search_in_description=1&keywords=DK%20Brother\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"title\": \"BUY tracks on TOONZSHOP\",\n" +
            "            \"network\": \"other\",\n" +
            "            \"url\": \"http://www.toonzshop.com/fr/catalog/search/recherche/Dk%20Brothers\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"title\": \"BUY tracks on JUNODOWNLOAD\",\n" +
            "            \"network\": \"other\",\n" +
            "            \"url\": \"http://www.junodownload.com/artists/Dk+Brothers/releases/\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"title\": \"\",\n" +
            "            \"network\": \"discogs\",\n" +
            "            \"url\": \"http://www.discogs.com/artist/DK+Brothers\"\n" +
            "          }\n" +
            "        ],\n" +
            "        \"_links\": {}\n" +
            "      },\n" +
            "      \"avatar_url_template\": \"https://i1.sndcdn.com/avatars-000276646158-p705v3-{size}.jpg\",\n" +
            "      \"station_urns\": [\n" +
            "        \"soundcloud:artist-stations:887221\"\n" +
            "      ],\n" +
            "      \"visual_url_template\": \"https://i1.sndcdn.com/visuals-000000887221-N6dcxP-{size}.jpg\",\n" +
            "      \"created_at\": \"2010/04/19 17:46:22 +0000\"\n" +
            "    }";
}
