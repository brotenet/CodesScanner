package application.controllers;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.imageio.ImageIO;

import com.github.sarxos.webcam.Webcam;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.Result;
import com.google.zxing.Writer;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;


public class FrmMain {
    private Webcam selected_web_cam = Webcam.getDefault();
    private HashMap<String, Webcam> available_web_cams;
    private boolean stop_webcam = false;
    private ObjectProperty<Image> image_property = new SimpleObjectProperty<>();
    private MultiFormatReader reader = new MultiFormatReader();
    private Clipboard clipboard = Clipboard.getSystemClipboard();
    private String webcam_scanned_value = "";
    private String image_scanned_value = "";
    private BarcodeFormat webcam_scanned_code_format;
    private BarcodeFormat image_scanned_code_format;
    private HashMap<String, Object> available_code_formats;
    private String tmp_file_path = System.getProperty("java.io.tmpdir") + File.separator + "CSG_4252b575-8a88-442b-9419-0e3f9854f996.png";
    
    @FXML ImageView ivWebCamView;
    @FXML ImageView ivImageView;
    @FXML ImageView ivCodeGenerationView;
    @FXML TextArea txtWebCamOutput;
    @FXML TextArea txtImageFileOutput;
    @FXML TextArea txtGenerationContent;
    @FXML ChoiceBox<String> cbAvailableWebCams;
    @FXML ChoiceBox<String> cbCodeFormats;
    @FXML Button btnSaveToImage;
    @FXML VBox cntWebCamScanCodeActions;
    @FXML VBox cntImageScanCodeActions;
    @FXML TabPane tabsMain;

    @FXML
    public void initialize() {
    	Rectangle web_cam_view_clip = new Rectangle(ivWebCamView.getFitWidth(), ivWebCamView.getFitHeight());
    	web_cam_view_clip.setArcWidth(20);
    	web_cam_view_clip.setArcHeight(20);
    	ivWebCamView.setClip(web_cam_view_clip);
    	Rectangle image_view_clip = new Rectangle(ivImageView.getFitWidth(), ivImageView.getFitHeight());
    	image_view_clip.setArcWidth(20);
    	image_view_clip.setArcHeight(20);
    	ivImageView.setClip(image_view_clip);
    	Rectangle code_gen_vie_clip = new Rectangle(ivCodeGenerationView.getFitWidth(), ivCodeGenerationView.getFitHeight());
    	code_gen_vie_clip.setArcWidth(20);
    	code_gen_vie_clip.setArcHeight(20);
    	ivCodeGenerationView.setClip(code_gen_vie_clip);
    	if(Webcam.getWebcams().size() > 0) {
    		loadWebCams();
            startWebCam();
    	}
    	loadCodeFormats();
    	setupDragAndDrop(ivImageView);
    	setupGenerationResets();
    }

    private void setupGenerationResets() {
		cbCodeFormats.addEventHandler(ActionEvent.ACTION, event -> { btnSaveToImage.setDisable(true); });
		txtGenerationContent.textProperty().addListener((observable) -> { btnSaveToImage.setDisable(true); }); //addEventHandler(ActionEvent.ACTION, event -> { btnSaveToImage.setDisable(true); });
	}

	private void loadCodeFormats() {
    	available_code_formats = new HashMap<>();
    	for(BarcodeFormat format : BarcodeFormat.values()) {
    		if(format != BarcodeFormat.MAXICODE && format != BarcodeFormat.RSS_14 && format != BarcodeFormat.RSS_EXPANDED && format != BarcodeFormat.UPC_EAN_EXTENSION) {
        		available_code_formats.put(format.name(), format);
        		cbCodeFormats.getItems().add(format.name());    			
    		}
    	};
    	cbCodeFormats.getSelectionModel().select(0);
	}

	private void loadWebCams() {
    	available_web_cams = new HashMap<>();
		for(Webcam web_cam : Webcam.getWebcams()) available_web_cams.put("/" + web_cam.getName().split("/", 2)[1].trim(), web_cam);
		for(String web_cam_name : available_web_cams.keySet()) cbAvailableWebCams.getItems().add(web_cam_name);
		cbAvailableWebCams.getSelectionModel().select(0);
		cbAvailableWebCams.setOnAction(event -> {
			String selected = cbAvailableWebCams.getValue();
			if(selected != null) switchWebCam(selected);
		});
	}

	protected void startWebCam() {
        Task<Void> web_cam_task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                if (selected_web_cam != null && selected_web_cam.isOpen()) {
                    stop_webcam = true;
                    selected_web_cam.close();
                }
                selected_web_cam.setViewSize(new Dimension(640, 480));
                selected_web_cam.open();
                startWebCamStream();
                return null;
            }
        };
        Thread web_cam_thread = new Thread(web_cam_task);
        web_cam_thread.setDaemon(true);
        web_cam_thread.start();
    }

    protected void startWebCamStream() {
        stop_webcam = false;
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                final AtomicReference<WritableImage> ref = new AtomicReference<>();
                BufferedImage img = null;
                int frame_count = 0; 
                while (!stop_webcam) {
                    try {
                        if ((img = selected_web_cam.getImage()) != null) {
                            WritableImage fx_img = SwingFXUtils.toFXImage(img, null);
                            ref.set(fx_img);
                            if (frame_count++ % 10 == 0) { processForBarcodeFromWebCam(fx_img, img); }
                            Platform.runLater(() -> image_property.set(ref.get()));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return null;
            }
        };
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
        ivWebCamView.imageProperty().bind(image_property);
    }

    private void processForBarcodeFromWebCam(WritableImage fx_image, BufferedImage awt_image) {
        try {
            BufferedImage webcam_image = new BufferedImage((int)fx_image.getWidth(), (int)fx_image.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
            java.awt.Graphics2D g2d = webcam_image.createGraphics();
            g2d.drawImage(awt_image, 0, 0, null);
            g2d.dispose();
            BinaryBitmap bin = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(webcam_image)));
            Result result = reader.decode(bin);
            if (result != null) {
                Platform.runLater(() -> {
                	cntWebCamScanCodeActions.setDisable(false);
                    txtWebCamOutput.setText(processText(result.getText(), result.getBarcodeFormat(), true));
                });
            }
        } catch (Exception e) {}
    }
    
    private void processForBarcodeFromImage(String path) {
        try {
            BufferedImage file_image = ImageIO.read(new File(path));
            BinaryBitmap bin = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(file_image)));
            Result result = reader.decode(bin);
            if (result != null) {
                Platform.runLater(() -> {
                	try {
                		cntImageScanCodeActions.setDisable(false);
						ivImageView.setImage(new Image(new FileInputStream(new File(path))));
						txtImageFileOutput.setText(processText(result.getText(), result.getBarcodeFormat(), false));
					} catch (FileNotFoundException e) {}
                });
            }
        } catch (Exception e) {
        	cntImageScanCodeActions.setDisable(true);
        	txtImageFileOutput.setText("No QR or Barcode found in image.");
        	ivImageView.setImage(new Image(getClass().getResourceAsStream("/resources/img/load_image.png")));
        }
    }

    private String processText(String text, BarcodeFormat format, boolean from_web_cam) {
		String output = text;
		if(from_web_cam) {
			webcam_scanned_value = text;
		}else {
			image_scanned_value = text;
		}
		if(text.startsWith("WIFI:")) {
			String wifi = "";
			String hidden = ""; if(text.split("H:")[1].split(";")[0].trim().equalsIgnoreCase("false")) hidden = " (hidden)";
			wifi += "SSID: " + text.split("S:")[1].split(";")[0] + hidden + "\n";
			wifi += "Password: " + text.split("P:")[1].split(";")[0] + "\n";
			wifi += "Security Type: " + text.split("T:")[1].split(";")[0];
			output = wifi;
		}
		if(format.name().trim().length() > 0) {
			if(from_web_cam) {
				webcam_scanned_code_format = format;
			}else {
				image_scanned_code_format = format;
			}
			output = output + "\n" + "Code Type: " + format;
		}
    	return output.trim();
	}

	private void stopWebCam() {
        stop_webcam = true;
        if (selected_web_cam != null && selected_web_cam.isOpen()) { selected_web_cam.close(); }
    }
    
    private void switchWebCam(String selected) {
    	stopWebCam();
    	selected_web_cam = available_web_cams.get(selected);
    	startWebCam();
    }
    
    @FXML
    private void sendWebCamToClipBoard() {
    	ClipboardContent content = new ClipboardContent();
    	content.putString(txtWebCamOutput.getText());
    	clipboard.setContent(content);
    }
    
    @FXML
    private void sendImageToClipBoard() {
    	ClipboardContent content = new ClipboardContent();
    	content.putString(txtImageFileOutput.getText());
    	clipboard.setContent(content);
	}
    
    @FXML
    private void sendWebCamToCodeGeneration() {
    	if(webcam_scanned_code_format != null) {
    		Boolean is_available = available_code_formats.keySet().contains(webcam_scanned_code_format.name());
    		if(is_available) {
    			ivCodeGenerationView.setImage(new Image(getClass().getResourceAsStream("/resources/img/generate_code.png")));
    			btnSaveToImage.setDisable(true);
    			cbCodeFormats.getSelectionModel().select(cbCodeFormats.getItems().indexOf(webcam_scanned_code_format.name()));
    			txtGenerationContent.setText(webcam_scanned_value);
    			tabsMain.getSelectionModel().select(2);
    		}
    	}
    }
    
    @FXML
    private void sendImageToCodeGeneration() {
    	if(image_scanned_code_format != null) {
    		Boolean is_available = available_code_formats.keySet().contains(image_scanned_code_format.name());
    		if(is_available) {
    			ivCodeGenerationView.setImage(new Image(getClass().getResourceAsStream("/resources/img/generate_code.png")));
    			btnSaveToImage.setDisable(true);
    			cbCodeFormats.getSelectionModel().select(cbCodeFormats.getItems().indexOf(image_scanned_code_format.name()));
    			txtGenerationContent.setText(image_scanned_value);
    			tabsMain.getSelectionModel().select(2);
    		}
    	}
	}
    
    @FXML
    private void generateCode() {
    	try {
    		String content = txtGenerationContent.getText();
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.MARGIN, 2);
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
            BarcodeFormat format = BarcodeFormat.valueOf(cbCodeFormats.getSelectionModel().getSelectedItem());
            Writer writer = new MultiFormatWriter();
            int width = 640;
            int height = 440;
            if(format == BarcodeFormat.QR_CODE || format == BarcodeFormat.AZTEC || format == BarcodeFormat.DATA_MATRIX || format == BarcodeFormat.PDF_417) {
            	height = 480;
            }        
            BitMatrix matrix = writer.encode(content, format, width, height);
            BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);
            BufferedImage final_image;
            if(format == BarcodeFormat.QR_CODE || format == BarcodeFormat.AZTEC || format == BarcodeFormat.DATA_MATRIX || format == BarcodeFormat.PDF_417) {
            	final_image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            }else {
            	final_image = new BufferedImage(width, height + 40, BufferedImage.TYPE_INT_RGB);
            }
            Graphics2D g2d = final_image.createGraphics();
            if(format != BarcodeFormat.QR_CODE && format != BarcodeFormat.AZTEC && format != BarcodeFormat.DATA_MATRIX && format != BarcodeFormat.PDF_417) { try {
            	g2d.setBackground(Color.WHITE);
            	g2d.clearRect(0, 0, width, height + 40);
                g2d.drawImage(image, 0, 0, null);
                g2d.setColor(Color.BLACK);
                g2d.setFont(new Font("Arial", Font.PLAIN, 20));
                int textX = width / 2;
                int textY = height + 30;
                g2d.drawString(content, textX - g2d.getFontMetrics().stringWidth(content)/2, textY);
            } finally { g2d.dispose(); }}
            else { try {
            	g2d.setBackground(Color.WHITE);
            	g2d.clearRect(0, 0, width, height);
            	g2d.drawImage(image, 0, 0, null);
            } finally { g2d.dispose(); }}
            ImageIO.write(final_image, "PNG", new File(tmp_file_path));
            ivCodeGenerationView.setImage(new Image("file:" + tmp_file_path));
            btnSaveToImage.setDisable(false);
    	}catch (Exception exception) {
    		Graphics2D g2d = null;
    		try {
    			int width = 640; int height = 480; String text = exception.getMessage();
    			BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        		g2d = image.createGraphics();
        		g2d.setColor(Color.WHITE);
                g2d.fillRect(0, 0, width, height);
                g2d.setColor(Color.BLACK);
                g2d.setFont(new Font("Arial", Font.PLAIN, 20));
                FontMetrics fm = g2d.getFontMetrics();
                int text_width = fm.stringWidth(text);
                int text_height = fm.getHeight();
                int x = (width - text_width) / 2;
                int y = (height + text_height) / 2;
                g2d.drawString(text, x, y);
                ImageIO.write(image, "PNG", new File(tmp_file_path));
                ivCodeGenerationView.setImage(new Image("file:" + tmp_file_path));
                btnSaveToImage.setDisable(true);
			} catch (Exception error) {
				g2d.dispose();
				btnSaveToImage.setDisable(true);
				error.printStackTrace();
			} finally {
				g2d.dispose();
			}
		}
    }
    
    @FXML
    private void saveGeneratedCode() throws Exception {
		FileChooser save_as_dalog = new FileChooser();
		save_as_dalog.setInitialFileName("generated_code.png");
		save_as_dalog.getExtensionFilters().add(new ExtensionFilter("PNG Image", "*.png"));
		File save_file = save_as_dalog.showSaveDialog(null);
		if(save_file != null) {
			Files.copy(Paths.get(tmp_file_path), Paths.get(save_file.getAbsolutePath()), StandardCopyOption.REPLACE_EXISTING);
		}
	}
    
    @FXML
    private void openImageFileLoader(MouseEvent event) {
		if(event.getClickCount() >= 2) {
			FileChooser dialog = new FileChooser();
			dialog.setTitle("Open a barcode/QRcode image file");
			dialog.getExtensionFilters().add(new ExtensionFilter("Image Files", "*.jpg", "*.jpeg", "*.png"));
			File file = dialog.showOpenDialog(null);
			if(file != null) processForBarcodeFromImage(file.getAbsolutePath());
		}
	}
    
    private void loadImageFile(DragEvent event) throws Exception {
    	String path = event.getDragboard().getFiles().get(0).getAbsolutePath();
    	processForBarcodeFromImage(path);
    }
    
    private void setupDragAndDrop(Node target_node) {
    	target_node.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY); 
                event.consume(); 
            } else {
                event.consume();
            }
        });
        target_node.setOnDragEntered(event -> {
             if (event.getDragboard().hasFiles()) {
                 event.consume();
             }
        });
        target_node.setOnDragDropped(event -> {
            if (event.getDragboard().hasFiles()) {
            	event.acceptTransferModes(TransferMode.COPY);
            	try {
					loadImageFile(event);
				} catch (Exception e) {
					e.printStackTrace();
				}
            }
            event.consume();
	    });
	}
}
