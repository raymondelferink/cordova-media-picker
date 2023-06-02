import UIKit
import MobileCoreServices
import AVFoundation

@objc(CordovaMediaPicker)
class CordovaMediaPicker: CDVPlugin {
    var commandCallback: String?
    var allowedOptions: Int = 0

    var allowCamera: Bool = false
    var allowGallery: Bool = false
    var allowVideo: Bool = false
    var allowFile: Bool = false
    var allowAudioRecorder: Bool = false
    var allowVideoRecorder: Bool = false

    var allowedDocumentTypes: [String] = []

    var cameraPickerBlock: ((_ base64: String) -> Void)? = nil
    var imagePickerBlock: ((_ image: URL) -> Void)? = nil
    var videoPickerBlock: ((_ data: URL) -> Void)? = nil
    var filePickerBlock: ((_ url: URL) -> Void)? = nil

    struct Constants {
        static let camera = "Camera"
        static let gallery = "Gallery"
        static let video = "Video"
        static let file = "File"
        static let audioRecorder = "Audio Recorder"
        static let videoRecorder = "Video Recorder"
        static let cancel = "Cancel"
    }

    static let shared: CordovaMediaPicker = CordovaMediaPicker() // Singleton Pattern
    fileprivate var currentViewController: UIViewController!

    override func pluginInitialize() {
        super.pluginInitialize()

        // Initialize default values
        allowCamera = false
        allowGallery = false
        allowVideo = false
        allowFile = false
        allowAudioRecorder = false
        allowVideoRecorder = false
    }

    @objc(pick:)
    func pick(command: CDVInvokedUrlCommand) {
        commandCallback = command.callbackId
        let options = command.arguments.first as AnyObject
        callPicker(options: options)
    }

    fileprivate func callPicker(options: AnyObject) {
        allowedOptions = 0
        allowedDocumentTypes.removeAll()
        
        // Initialize default values
        allowCamera = false
        allowGallery = false
        allowVideo = false
        allowFile = false
        allowAudioRecorder = false
        allowVideoRecorder = false

        if let cameraOption = options["camera"] as? Int, cameraOption == 1 {
            allowCamera = true
            allowedOptions += 1
        }
        if let galleryOption = options["gallery"] as? Int, galleryOption == 1 {
            allowGallery = true
            allowedOptions += 1
        }
        if let videoOption = options["video"] as? Int, videoOption == 1 {
            allowVideo = true
            allowedOptions += 1
        }
        if let fileOption = options["file"] as? Int, fileOption == 1 {
            allowFile = true
            allowedOptions += 1
        }
        if let audioRecorderOption = options["audiorecorder"] as? Int, audioRecorderOption == 1 {
            allowAudioRecorder = true
            allowedOptions += 1
        }
        if let videoRecorderOption = options["videorecorder"] as? Int, videoRecorderOption == 1 {
            allowVideoRecorder = true
            allowedOptions += 1
        }

        if allowedOptions == 0 {
            allowCamera = true
            allowGallery = true
            allowVideo = true
            allowFile = true
            allowAudioRecorder = true
            allowVideoRecorder = true
            allowedOptions = 6
        }

        let fileTypeOptions = options["filetypes"] as? [String: Int] ?? [:]
        var allowedMimes = 0

        if fileTypeOptions["photo"] == 1 {
            allowedDocumentTypes.append(kUTTypeImage as String)
            allowedMimes += 1
        }
        if fileTypeOptions["video"] == 1 {
            allowedDocumentTypes.append(kUTTypeMovie as String)
            allowedDocumentTypes.append(kUTTypeVideo as String)
            allowedMimes += 1
        }
        if fileTypeOptions["audio"] == 1 {
            allowedDocumentTypes.append(kUTTypeMP3 as String)
            allowedDocumentTypes.append(kUTTypeAudio as String)
            allowedMimes += 1
        }
        if (fileTypeOptions["file"] == 1) {
            allowedDocumentTypes.append(kUTTypePDF as String)
            allowedDocumentTypes.append(kUTTypePlainText as String)
            allowedMimes += 1
        }

        if allowedMimes == 0 {
            allowedDocumentTypes.append(kUTTypeImage as String)
            allowedDocumentTypes.append(kUTTypeMovie as String)
            allowedDocumentTypes.append(kUTTypeVideo as String)
            allowedDocumentTypes.append(kUTTypeMP3 as String)
            allowedDocumentTypes.append(kUTTypeAudio as String)
            allowedDocumentTypes.append(kUTTypePDF as String)
            allowedDocumentTypes.append(kUTTypePlainText as String)
            allowedMimes = 7
        }

        //Receive Image
        self.cameraPickerBlock = { (base64) -> Void in
            do {
                
                let result = ([["base64": base64, "type": "image/jpeg"]])
                if let message = try String(
                    data: JSONSerialization.data(
                        withJSONObject: result,
                        options: []
                    ),
                    encoding: String.Encoding.utf8
                ) {
                    self.send(message)
                }
                else {
                    self.sendError("Serializing result failed.")
                }
            }
            catch let error {
                self.sendError(error.localizedDescription)
            }
        }
        //Receive Image
        self.imagePickerBlock = { (file) -> Void in
            do {
                let fileData = try! Data.init(contentsOf: file)

                let mimeType = self.detectMimeType(file)
                var base64String: String = ""
                if (mimeType.starts(with: "image")) {
                    base64String = fileData.base64EncodedString(options: NSData.Base64EncodingOptions.init(rawValue: 0))
                }
                let result = ([["uri": file.absoluteString, "base64": base64String, "name": file.lastPathComponent, "type": mimeType]])

                if let message = try String(
                    data: JSONSerialization.data(
                        withJSONObject: result,
                        options: []
                    ),
                    encoding: String.Encoding.utf8
                ) {
                    self.send(message)
                }
                else {
                    self.sendError("Serializing result failed.")
                }
            }
            catch let error {
                self.sendError(error.localizedDescription)
            }
        }
        //Receive Video
        self.videoPickerBlock = { (file) -> Void in
            do {
                let fileData = try! Data.init(contentsOf: file)

                let mimeType = self.detectMimeType(file)
                var base64String: String = ""
                if (mimeType.starts(with: "image")) {
                    base64String = fileData.base64EncodedString(options: NSData.Base64EncodingOptions.init(rawValue: 0))
                }
                let result = ([["uri": file.absoluteString, "base64": base64String, "name": file.lastPathComponent, "type": mimeType]])

                if let message = try String(
                    data: JSONSerialization.data(
                        withJSONObject: result,
                        options: []
                    ),
                    encoding: String.Encoding.utf8
                ) {
                    self.send(message)
                }
                else {
                    self.sendError("Serializing result failed.")
                }
            }
            catch let error {
                self.sendError(error.localizedDescription)
            }
        }
        //Receive File
        self.filePickerBlock = { (file) -> Void in
            do {
                let fileData = try! Data.init(contentsOf: file)

                let mimeType = self.detectMimeType(file)
                var base64String: String = ""
                if (mimeType.starts(with: "image")) {
                    base64String = fileData.base64EncodedString(options: NSData.Base64EncodingOptions.init(rawValue: 0))
                }
                let result = ([["uri": file.absoluteString, "base64": base64String, "name": file.lastPathComponent, "type": mimeType]])

                if let message = try String(
                    data: JSONSerialization.data(
                        withJSONObject: result,
                        options: []
                    ),
                    encoding: String.Encoding.utf8
                ) {
                    self.send(message)
                }
                else {
                    self.sendError("Serializing result failed.")
                }
            }
            catch let error {
                self.sendError(error.localizedDescription)
            }
        }
        
        
        
        if allowedOptions > 1 {
            showActionSheet()
        } else if (allowCamera) {
            self.showImagePicker(sourceType: .camera)
        } else if (allowGallery) {
            self.showImagePicker(sourceType: .photoLibrary)
        } else if (allowVideo) {
            self.showVideoPicker(sourceType: .photoLibrary)
        } else if (self.allowFile) {
            self.showDocumentPicker()
        } else if (self.allowAudioRecorder) {
            self.showAudiorecorder()
        } else if (self.allowVideoRecorder) {
            self.showVideorecorder()
        } else {
            self.sendError("No options allowed")
        }
    }

    fileprivate func showActionSheet() {
        let actionSheet = UIAlertController(title: nil, message: nil, preferredStyle: .actionSheet)

        if allowCamera {
            let cameraAction = UIAlertAction(title: Constants.camera, style: .default) { (_) in
                self.showImagePicker(sourceType: .camera)
            }
            actionSheet.addAction(cameraAction)
        }

        if allowGallery {
            let galleryAction = UIAlertAction(title: Constants.gallery, style: .default) { (_) in
                self.showImagePicker(sourceType: .photoLibrary)
            }
            actionSheet.addAction(galleryAction)
        }

        if allowVideo {
            let videoAction = UIAlertAction(title: Constants.video, style: .default) { (_) in
                self.showVideoPicker(sourceType: .photoLibrary)
            }
            actionSheet.addAction(videoAction)
        }

        if allowFile {
            let fileAction = UIAlertAction(title: Constants.file, style: .default) { (_) in
                self.showDocumentPicker()
            }
            actionSheet.addAction(fileAction)
        }

        if allowAudioRecorder {
            let audioRecorderAction = UIAlertAction(title: Constants.audioRecorder, style: .default) { (_) in
                // Start audio recorder
                self.showAudiorecorder();
            }
            actionSheet.addAction(audioRecorderAction)
        }

        if allowVideoRecorder {
            let videoRecorderAction = UIAlertAction(title: Constants.videoRecorder, style: .default) { (_) in
                // Start video recorder
                self.showVideorecorder();
            }
            actionSheet.addAction(videoRecorderAction)
        }

        let cancelAction = UIAlertAction(title: Constants.cancel, style: .cancel) { (_) in
            self.commandCallback = nil
        }
        actionSheet.addAction(cancelAction)

        if let popoverPresentationController = actionSheet.popoverPresentationController {
            popoverPresentationController.sourceView = self.viewController.view
            popoverPresentationController.sourceRect = self.viewController.view.bounds
            popoverPresentationController.permittedArrowDirections = []
        }

        self.viewController.present(actionSheet, animated: true, completion: nil)
    }
    
    fileprivate func showDocumentPicker() {
        let documentPicker = UIDocumentPickerViewController(documentTypes: allowedDocumentTypes, in: .import)
        documentPicker.delegate = self
        documentPicker.modalPresentationStyle = .formSheet
        self.viewController.present(documentPicker, animated: true, completion: nil)
    }

    fileprivate func showImagePicker(sourceType: UIImagePickerController.SourceType) {
        let imagePicker = UIImagePickerController()
        imagePicker.sourceType = sourceType
        imagePicker.delegate = self
        imagePicker.mediaTypes = [kUTTypeImage as String]
        imagePicker.modalPresentationStyle = .fullScreen
        self.viewController.present(imagePicker, animated: true, completion: nil)
    }

    fileprivate func showVideoPicker(sourceType: UIImagePickerController.SourceType) {
        let videoPicker = UIImagePickerController()
        videoPicker.sourceType = sourceType
        videoPicker.delegate = self
        videoPicker.mediaTypes = [kUTTypeMovie as String, kUTTypeVideo as String]
        videoPicker.modalPresentationStyle = .fullScreen
        self.viewController.present(videoPicker, animated: true, completion: nil)
    }
    
    fileprivate func showAudiorecorder() {
        // use external audiorecorder
        self.send("OPEN_AUDIORECORDER")
    }
    
    fileprivate func showVideorecorder() {
        if UIImagePickerController.isSourceTypeAvailable(.camera) {
            let pickerController = UIImagePickerController()
            pickerController.delegate = self;
            pickerController.sourceType = .camera;
            pickerController.mediaTypes = [kUTTypeMovie as String]
            pickerController.cameraCaptureMode = .video;
            pickerController.videoMaximumDuration = 30.0;
            self.viewController.present(pickerController, animated: true,   completion: nil)
        }
    }
    
    func detectMimeType (_ url: URL) -> String {
        if let uti = UTTypeCreatePreferredIdentifierForTag(
            kUTTagClassFilenameExtension,
            url.pathExtension as CFString,
            nil
        )?.takeRetainedValue() {
            if let mimetype = UTTypeCopyPreferredTagWithClass(
                uti,
                kUTTagClassMIMEType
            )?.takeRetainedValue() as String? {
                return mimetype
            }
        }

        return "application/octet-stream"
    }
    
    func send (_ message: String, _ status: CDVCommandStatus = CDVCommandStatus_OK) {
        if let callbackId = self.commandCallback {
            self.commandCallback = nil
            
            let pluginResult = CDVPluginResult(
                status: status,
                messageAs: message
            )

            self.commandDelegate!.send(
                pluginResult,
                callbackId: callbackId
            )
            
        }
    }
    
    func sendError (_ message: String, _ status: CDVCommandStatus = CDVCommandStatus_ERROR) {
        if let callbackId = self.commandCallback {
            self.commandCallback = nil

            let pluginResult = CDVPluginResult(
                status: status,
                messageAs: message
            )

            self.commandDelegate!.send(
                pluginResult,
                callbackId: callbackId
            )
        }
    }
}

extension CordovaMediaPicker: UIImagePickerControllerDelegate, UINavigationControllerDelegate {
    func imagePickerController(_ picker: UIImagePickerController, didFinishPickingMediaWithInfo info: [String : Any]) {
        picker.dismiss(animated: true, completion: nil)

        if let image = info[UIImagePickerControllerImageURL] as? URL {
            imagePickerBlock?(image) //return image when not null
        }
        else if let videoUrl = info[UIImagePickerControllerMediaURL] as? URL {
            //let data = try? Data(contentsOf: videoUrl)
            let spliteArray = videoUrl.pathComponents
            let lastString = spliteArray.last ?? ""
            let fileManager = FileManager.default
            let documentsDirectory = FileManager.default.temporaryDirectory
            let filePath = documentsDirectory.appendingPathComponent(lastString)
            do {
                try fileManager.copyItem(at: videoUrl, to: filePath)
                videoPickerBlock?(filePath) //return video url when not null
            } catch {
                print("Something went wrong")
            }
            
        }
        else if let image = info[UIImagePickerControllerOriginalImage] as? UIImage {
            let strBase64:String = UIImageJPEGRepresentation(image, 1)?.base64EncodedString() ?? ""
            cameraPickerBlock?(strBase64) //return image when not null
        }
        else{
            print("Something went wrong")
        }
    }

    func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
        picker.dismiss(animated: true, completion: nil)
        commandCallback = nil
    }
}



extension CordovaMediaPicker: UIDocumentPickerDelegate {
    func documentPicker(_ controller: UIDocumentPickerViewController, didPickDocumentsAt urls: [URL]) {
        controller.dismiss(animated: true, completion: nil)
        if let fileURL = urls.first {
            filePickerBlock?(fileURL)
        }
    }

    func documentPickerWasCancelled(_ controller: UIDocumentPickerViewController) {
        controller.dismiss(animated: true, completion: nil)
        commandCallback = nil
    }
}
