package com.soundcloud.android;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.EventBus;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;


@RunWith(SoundCloudTestRunner.class)
public class SoundCloudApplicationTest {

    @Mock
    private EventBus eventBus;
    @Mock
    private AccountOperations accountOperations;

    @Test
    public void shouldOnlyHaveOneLauncherActivity() throws Exception {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(true);
        Document doc = f.newDocumentBuilder().parse(SoundCloudTestRunner.MANIFEST);
        NodeList nl = (NodeList) XPathFactory.newInstance().newXPath().compile("//activity/intent-filter/category").evaluate(doc, XPathConstants.NODESET);
        int launchers = 0;
        for (int i = 0; i < nl.getLength(); i++) {
            Node n = nl.item(i);
            NamedNodeMap attributes = n.getAttributes();
            if (attributes != null) {
                Node name = attributes.getNamedItem("android:name");
                if (name != null && "android.intent.category.LAUNCHER".equals(name.getNodeValue())) {
                    launchers++;
                }
            }
        }
        expect(launchers).toEqual(1);
    }

    @Test
    public void shouldHaveOpenGLEnabled() throws Exception {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(true);
        Document doc = f.newDocumentBuilder().parse(SoundCloudTestRunner.MANIFEST);
        NodeList nl = (NodeList) XPathFactory.newInstance().newXPath().compile("//application").evaluate(doc, XPathConstants.NODESET);
        expect(nl.getLength()).toEqual(1);
        Node app = nl.item(0);
        expect(app.getAttributes().getNamedItem("android:hardwareAccelerated").getNodeValue()).toEqual("true");
    }
}
