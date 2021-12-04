import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.fit.pdfdom.PDFDomTree;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

public class PdfConverter {

  public PdfConverter() {}

  public String handleInput(String action, String fileUrl) {
    try {
      String path;
      path = DownloadFile(fileUrl);
      if(!path.equals("work")){
        switch (action) {
          case "ToImage":
            convertToImage(path, "src/output/");
            break;
          case "ToHTML":
            convertToHTML(path, "src/output/html.html");
            break;
          case "ToText":
            convertToText(path, "src/output/pdf_as_text.txt");
            break;
          default:
            //do nothing
            break;
        }
      }

    }
    catch (Exception e) {
      e.printStackTrace();
    }
    //todo: return converted as string
    return;
  }

  private static String DownloadFile(String filePath) throws IOException{
    String ret = "src/output/pdf.pdf";
    URL url = new URL(filePath);
    InputStream is = url.openStream();
    ReadableByteChannel channel = Channels.newChannel( url.openStream());
    FileOutputStream fo = new FileOutputStream(ret);
    fo.getChannel().transferFrom(channel, 0, Long.MAX_VALUE);
    fo.close();
    channel.close();
    return ret;
  }

  private static void convertToImage(String inputPath, String outputPath) throws IOException {
    PDDocument document = PDDocument.load(new File(inputPath));
    PDFRenderer pdfRenderer = new PDFRenderer(document);

    for (int page = 0; page < 1 /*document.getNumberOfPages() */; ++page) {
      BufferedImage bim = pdfRenderer.renderImageWithDPI(
        page, 300, ImageType.RGB);
      ImageIOUtil.writeImage(
        bim, String.format("src/output/pdf-%d.%s", page + 1, "jpg"), 300);
    }
    document.close();
  }

  private static void convertToText(String inputPath, String outputPath) throws IOException {
    PDDocument document = PDDocument.load(new File(inputPath));
    AccessPermission ap = document.getCurrentAccessPermission();
    if (!ap.canExtractContent())
      throw new IOException("Dont have permissions");

    PDFTextStripper stripper = new PDFTextStripper();
    stripper.setSortByPosition(true);

    for (int p = 1; p < 2 /*document.getNumberOfPages()*/; ++p) {
      stripper.setStartPage(p);
      stripper.setEndPage(p);
      String text = stripper.getText(document);

      try {
        PrintWriter pw = new PrintWriter(outputPath);
        pw.print(text);
        pw.close();
      } catch (FileNotFoundException e) {
        System.out.println("File not found");
      }
    }
  }

  private static void convertToHTML(String inputPath, String outputPath) throws IOException, ParserConfigurationException {
    PDDocument doc = PDDocument.load(new File(inputPath));
    AccessPermission ap = doc.getCurrentAccessPermission();
    if (!ap.canExtractContent()){throw new IOException("Dont have permissions");}
    PDFTextStripper stripper = new PDFTextStripper();
    stripper.setSortByPosition(true);
    stripper.setStartPage(1);
    stripper.setEndPage(1);
    String text = stripper.getText(doc);
    PrintWriter pw = new PrintWriter(outputPath);
    PDFDomTree p = new PDFDomTree();
    p.setEndPage(1);
    p.writeText(doc, pw);
    doc.close();
  }

}