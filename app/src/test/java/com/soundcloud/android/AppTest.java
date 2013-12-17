package com.soundcloud.android;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.File;

import static com.soundcloud.android.Expect.expect;


@RunWith(DefaultTestRunner.class)
public class AppTest {

    @Test
    public void shouldOnlyHaveOneLauncherActivity() throws Exception {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(true);
        Document doc = f.newDocumentBuilder().parse(new File("../app/AndroidManifest.xml"));
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
        Document doc = f.newDocumentBuilder().parse(new File("../app/AndroidManifest.xml"));
        NodeList nl = (NodeList) XPathFactory.newInstance().newXPath().compile("//application").evaluate(doc, XPathConstants.NODESET);
        expect(nl.getLength()).toEqual(1);
        Node app = nl.item(0);
        expect(app.getAttributes().getNamedItem("android:hardwareAccelerated").getNodeValue()).toEqual("true");
    }
}
