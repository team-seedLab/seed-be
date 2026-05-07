package com.example.seedbe.domain.project.component.pdf;

import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class PdfImageDetector {

    public boolean hasImage(PDPage page) throws IOException {
        return hasImage(page.getResources());
    }

    private boolean hasImage(PDResources resources) throws IOException {
        if (resources == null) {
            return false;
        }

        for (COSName name : resources.getXObjectNames()) {
            PDXObject xObject = resources.getXObject(name);
            if (xObject instanceof PDImageXObject) {
                return true;
            }
            if (xObject instanceof PDFormXObject form && hasImage(form.getResources())) {
                return true;
            }
        }

        return false;
    }
}
