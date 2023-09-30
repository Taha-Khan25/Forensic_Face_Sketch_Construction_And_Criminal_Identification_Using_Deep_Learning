package thirdeye.v2;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClientBuilder;
import com.amazonaws.services.rekognition.model.FaceMatch;
import com.amazonaws.services.rekognition.model.S3Object;
import com.amazonaws.services.rekognition.model.SearchFacesByImageRequest;
import com.amazonaws.services.rekognition.model.SearchFacesByImageResult;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FaceRecognitionController implements Initializable {

    @FXML
    private Button open_sketch;
    @FXML
    private Button upload_sketch;
    @FXML
    private Button find_match;

    @FXML
    private Button draw_btn;
    @FXML
    private ImageView sketch_path;
    @FXML
    private ImageView match;

    @FXML
    private TextArea match_properties;

    @FXML
    private Label match_similarity;

    @FXML
    private Label sketch_location;

    @FXML
    private Label match_path;

    @FXML
    private Label status;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        open_sketch.setOnAction(new EventHandler<>() {
            @Override
            public void handle(ActionEvent event) {
                open_the_sketch(event);
            }
        });

        upload_sketch.setOnAction(new EventHandler<>() {
            @Override
            public void handle(ActionEvent event) {
                upload_sketchActionPerformed(event);
            }
        });

        find_match.setOnAction(new EventHandler<>() {
            @Override
            public void handle(ActionEvent event) {
                find_matchActionPerformed(event);
            }
        });

        draw_btn.setOnAction(new EventHandler<>() {
            @Override
            public void handle(ActionEvent event) {
                draw_sketch(event);
            }
        });
    }

    private void open_the_sketch(ActionEvent t) {
        String loc="C:\\Users\\tahak\\Downloads";
        FileChooser fileChooser = new FileChooser();

        //Set extension filter
        FileChooser.ExtensionFilter extFilterJPG = new FileChooser.ExtensionFilter("JPG files (*.jpg)", "*.JPG");
        FileChooser.ExtensionFilter extFilterPNG = new FileChooser.ExtensionFilter("PNG files (*.png)", "*.PNG");
        fileChooser.getExtensionFilters().addAll(extFilterJPG, extFilterPNG);

        //Show open file dialog
        File file = fileChooser.showOpenDialog(null);

        try {
            BufferedImage bufferedImage = ImageIO.read(file);
            String getselectedImage = file.getAbsolutePath();
            System.out.println(getselectedImage);
            sketch_location.setText(getselectedImage);

            Image image = SwingFXUtils.toFXImage(bufferedImage, null);
            status.setText("Image Selected Succesfully");
            status.setTextFill(Color.rgb(8, 204, 31));
            sketch_path.setImage(image);
        } catch (IOException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void upload_sketchActionPerformed(ActionEvent event) {
        Regions clientRegion = Regions.US_EAST_1;
        String bucketName = "thirdeyes3";
        String fileObjKeyName = "test.jpg";
        String fileName = sketch_location.getText();
        try {

            AmazonS3 s3Client = AmazonS3ClientBuilder
                    .standard()
                    .withRegion(clientRegion)
                    .build();

            // Upload a file as a new object with ContentType and title specified.
            PutObjectRequest request = new PutObjectRequest(bucketName, fileObjKeyName, new File(fileName));
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType("plain/text");
            metadata.addUserMetadata("title", "someTitle");
            request.setMetadata(metadata);
            s3Client.putObject(request);

            status.setText("Image Uploaded Succesfully");
            status.setTextFill(Color.rgb(8, 204, 31));
            System.out.println("Image Uploaded Succesfully");
            Alert alert=new Alert(Alert.AlertType.INFORMATION);
            alert.setContentText("Sketch Uploaded Successfully");
            alert.show();

            match_path.setText(null); //Clears the old path data of the image
            match.setImage(null); //Clears the Old Matched Image
            match_similarity.setText(null); //Clears the Similarity Percentage
            match_properties.setText(null);

        } catch (AmazonServiceException e) {
            e.printStackTrace();
            status.setText("Upload Failed");
            status.setTextFill(Color.rgb(201, 2, 2));
            System.out.println("The Sketch could not been Uploaded, Try Again");
            Alert alert=new Alert(Alert.AlertType.ERROR);
            alert.setContentText("\"The Sketch could not been Uploaded, Try Again");
            alert.show();
            throw new RuntimeException(e);

        } catch (SdkClientException e) {

            // The call was transmitted successfully, but Amazon S3 couldn't process
            // it, so it returned an error response.
            status.setText("Upload Failed");
            status.setTextFill(Color.rgb(201, 2, 2));
            System.out.println("The Sketch could not been Uploaded, Try Again");
            e.printStackTrace();
            Alert alert=new Alert(Alert.AlertType.ERROR);
            alert.setContentText("The Sketch could not been Uploaded, Try Again");
            alert.show();
        }
    }


    private void find_matchActionPerformed(ActionEvent evt) {//GEN-FIRST:event_find_matchActionPerformed
        String collectionId = "Records";
        String bucket = "thirdeyes3";
        String photo = "test.jpg";

        AmazonRekognition rekognitionClient = AmazonRekognitionClientBuilder.defaultClient();

        ObjectMapper objectMapper = new ObjectMapper();

        // Get an image object from S3 bucket.
        com.amazonaws.services.rekognition.model.Image image=new com.amazonaws.services.rekognition.model.Image()
                .withS3Object(new S3Object()
                        .withBucket(bucket)
                        .withName(photo));

        // Search collection for faces similar to the largest face in the image.
        SearchFacesByImageRequest searchFacesByImageRequest = new SearchFacesByImageRequest()
                .withCollectionId(collectionId)
                .withImage(image)
                .withFaceMatchThreshold(70F)
                .withMaxFaces(2);

        SearchFacesByImageResult searchFacesByImageResult =
                rekognitionClient.searchFacesByImage(searchFacesByImageRequest);


        List<FaceMatch> faceImageMatches = searchFacesByImageResult.getFaceMatches();
        faceImageMatches.forEach((FaceMatch face) -> {
            try {
                System.out.println(objectMapper.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(face));

                match_path.setText(face.getFace().getExternalImageId()); //Used for verifing and No Match

                //Display the Similarity Percentage on the Screen
                match_similarity.setText("SIMILARITY : "+face.getSimilarity());

                status.setText("Match Found");
                status.setTextFill(Color.rgb(8, 204, 31));


                // Display Properties\
                System.out.println("MATCH FOUND");
                match_properties.setText("*****************"+
                        "FACE MATCHED"+
                        "***************** \n"+
                        "Name in database: "+face.getFace().getExternalImageId() + "\n" +
                        "Similarity: "+face.getSimilarity() + "\n" +
                        "Confidence: "+face.getFace().getConfidence() + "\n" );

                // Display Matched Images in the JLABEL
                String path = "https://thirdeyes3.s3.us-east-1.amazonaws.com/"+face.getFace().getExternalImageId();
                System.out.println("Get Image from " + path);
                try {
                    URL url = new URL(path);
                    //ImageIcon imageIcon=new ImageIcon(new ImageIcon(url).getImage().getScaledInstance(200,300, java.awt.Image.SCALE_SMOOTH));
                    System.out.println(path);
                    Image image1=new Image(path);
                    match.setImage(image1);
                } catch (IOException ignored) {

                }
            } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            }
        });

        if (match_path.getText()==null) { // IF THE SKETCH DOES NOT MATCH
            System.out.println("NO MATCH FOUND");
            status.setText("No Match Found");
            status.setTextFill(Color.rgb(201, 2, 2));
            Alert alert=new Alert(Alert.AlertType.ERROR);
            alert.setContentText("\"No Match Found in the Database !!");
            alert.show();
            match.setImage(null);
        }
    }

    @FXML //Open upload sketch window
    private void draw_sketch(ActionEvent event) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader();
            fxmlLoader.setLocation(getClass().getResource("dashboard.fxml"));
            Scene scene = new Scene(fxmlLoader.load());
            Stage stage = new Stage();
            stage.setTitle("Face Sketch Constructor");
            stage.setScene(scene);
            stage.resizableProperty().setValue(false); //Disable maximize button
            stage.show();
            ((Node)(event.getSource())).getScene().getWindow().hide();

        } catch (IOException e) {
            Logger logger = Logger.getLogger(getClass().getName());
        }
    }
}






