import UIKit
import MobileCoreServices
import Foundation
import AVFoundation

@objc(CordovaMediaPicker) class CordovaMediaPicker : CDVPlugin {
    var commandCallback: String?

    var allowCamera = false
    var allowGallery = false
    var allowVideo = false
    var allowFile = false
    var allowAudioRecorder = false
    var allowVideoRecorder = false
    var allowedOptions = 0;
    
    var lastInfo: [String : Any]?
    var allowedDocumentTypes: [String : Any]?

    struct Constants {
        static let camera = "Camera"
        static let gallery = "Gallery"
        static let video = "Video"
        static let file = "File"
        static let audiorecorder = "Audio Recorder"
        static let videorecorder = "Video Recorder"
        static let cancel = "Cancel"
        //static let documentTypes = ["com.microsoft.word.doc", "public.data", "org.openxmlformats.wordprocessingml.document", kUTTypePDF as String] //Use for specify type you need to pickup
        static let documentTypes = [
            kUTTypeImage as String, 
            kUTTypeMovie as String, 
            kUTTypeVideo as String, 
            kUTTypeMP3 as String, 
            kUTTypeAudio as String,
            kUTTypePDF as String, 
            kUTTypePlainText as String
        ]
    }

    static let shared: CordovaMediaPicker = CordovaMediaPicker() //Singleton Pattern
    fileprivate var currentViewController: UIViewController!
    var cameraPickerBlock: ((_ base64: String) -> Void)?
    var imagePickerBlock: ((_ image: URL) -> Void)?
    var videoPickerBlock: ((_ data: URL) -> Void)?
    var filePickerBlock: ((_ url: URL) -> Void)?

    func callPicker (options: NSArray) {
        self.allowedOptions = 0;
        self.allowCamera = false;
        self.allowGallery = false;
        self.allowVideo = false;
        self.allowFile = false;
        self.allowAudio = false;
        self.allowAudioRecorder = false;
        self.allowVideoRecorder = false;

        self.allowCamera = (options["camera"] as! Int == 1);
        if (self.allowCamera) {self.allowedOptions+=1}
        self.allowGallery = (options["gallery"] as! Int == 1);
        if (self.allowGallery) {self.allowedOptions+=1}
        self.allowVideo = (options["video"] as! Int == 1);
        if (self.allowVideo) {self.allowedOptions+=1}
        self.allowFile = (options["file"] as! Int == 1);
        if (self.allowFile) {self.allowedOptions+=1}
        self.allowAudioRecorder = (options["audiorecorder"] as! Int == 1);
        if (self.allowAudioRecorder) {self.allowedOptions+=1}
        self.allowVideoRecorder = (options["videorecorder"] as! Int == 1);
        if (self.allowVideoRecorder) {self.allowedOptions+=1}
        
        if (self.allowedOptions == 0) {
            self.allowCamera = true;
            self.allowGallery = true;
            self.allowVideo = true;
            self.allowFile = true;
            self.allowAudioRecorder = true;
            self.allowVideoRecorder = true;
            self.allowedOptions = 6
        }

        var allowedmimes = 0;
        let filetypeoptions = options["filetypes"] as! AnyObject
        if (filetypeoptions["photo"] as! Int == 1) {
            self.allowedDocumentTypes.append(kUTTypeImage as String);
            allowedmimes+=1;
        }
        if (filetypeoptions["video"] as! Int == 1) {
            self.allowedDocumentTypes.append(kUTTypeMovie as String);
            self.allowedDocumentTypes.append(kUTTypeVideo as String);
            allowedmimes+=1;
        }
        if (filetypeoptions["audio"] as! Int == 1) {
            self.allowedDocumentTypes.append(kUTTypeMP3 as String);
            self.allowedDocumentTypes.append(kUTTypeAudio as String);
            allowedmimes+=1;
        }
        if (filetypeoptions["file"] as! Int == 1) {
            self.allowedDocumentTypes.append(kUTTypePDF as String);
            self.allowedDocumentTypes.append(kUTTypePlainText as String);
            allowedmimes+=1;
        }
        if (allowedmimes == 0) {
            // allow all mime types when none are set in options
            self.allowedDocumentTypes = Constants.documentTypes;
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

        if (self.allowedOptions > 1) {
            self.showActionSheet(viewController: self.viewController)
        } else if (self.allowCamera) {
            self.camera()
        } else if (self.allowGallery) {
            self.photoLibrary()
        } else if (self.allowVideo) {
            self.video()
        } else if (self.allowFile) {
            self.file()
        } else if (self.allowAudioRecorder) {
            self.audiorecorder()
        } else if (self.allowVideoRecorder) {
            self.videorecorder()
        } else {
            self.sendError("No options allowed")
        }
    }
    @objc(pick:)
    func pick(command: CDVInvokedUrlCommand) {
        self.commandCallback = command.callbackId
        let options = command.arguments.first as! AnyObject

        self.callPicker(options: options)
    }


    fileprivate func camera() {
       if UIImagePickerController.isSourceTypeAvailable(.camera) {
          let pickerController = UIImagePickerController()
          pickerController.delegate = self;
          pickerController.sourceType = .camera
          currentViewController.present(pickerController, animated: true,   completion: nil)
       }
    }

    fileprivate func photoLibrary() {
       if UIImagePickerController.isSourceTypeAvailable(.photoLibrary) {
          let pickerController = UIImagePickerController()
          pickerController.delegate = self;
          pickerController.sourceType = .photoLibrary
          currentViewController.present(pickerController, animated: true, completion: nil)
       }
    }

    fileprivate func video() {
        if UIImagePickerController.isSourceTypeAvailable(.photoLibrary) {
           let pickerController = UIImagePickerController()
           pickerController.delegate = self
           pickerController.sourceType = .photoLibrary
           pickerController.mediaTypes = [kUTTypeMovie as String, kUTTypeVideo as String]
           currentViewController.present(pickerController, animated: true, completion: nil)
        }
    }

    fileprivate func file() {
       let importMenuViewController = UIDocumentPickerViewController(documentTypes: self.allowedDocumentTypes, in: .import)
       importMenuViewController.delegate = self
       importMenuViewController.modalPresentationStyle = .formSheet
       currentViewController.present(importMenuViewController, animated: true, completion: nil)
    }

    fileprivate func audiorecorder() {
        // use external audiorecorder
        self.send("OPEN_AUDIORECORDER")
    }

    fileprivate func videorecorder() {
        if UIImagePickerController.isSourceTypeAvailable(.camera) {
            let pickerController = UIImagePickerController()
            pickerController.delegate = self;
            pickerController.sourceType = .camera;
            pickerController.mediaTypes = [kUTTypeMovie as String]
            pickerController.cameraCaptureMode = .video;
            currentViewController.present(pickerController, animated: true,   completion: nil)
        }
    }

    func showActionSheet(viewController: UIViewController) {
        currentViewController = viewController
        
        // fix for ipad from: https://stackoverflow.com/a/60403127
        var alertStyle = UIAlertController.Style.actionSheet
        if (UIDevice.current.userInterfaceIdiom == .pad) {
            alertStyle = UIAlertController.Style.alert
        }
        let actionSheet = UIAlertController(title: nil, message: nil, preferredStyle: alertStyle)

        if (self.allowCamera) {
            let camera = UIAlertAction(title: Constants.camera, style: .default, handler: { (action) -> Void in
               self.camera()
            })
            actionSheet.addAction(camera)
        }

        if (self.allowGallery) {
            let gallery = UIAlertAction(title: Constants.gallery, style: .default, handler: { (action) -> Void in
                self.photoLibrary()
            })
            actionSheet.addAction(gallery)
        }

        if (self.allowVideo) {
            let video = UIAlertAction(title: Constants.video, style: .default, handler: { (action) -> Void in
                self.video()
            })
            actionSheet.addAction(video)
        }

        if (self.allowFile) {
            let file = UIAlertAction(title: Constants.file, style: .default, handler: { (action) -> Void in
                self.file()
            })
            actionSheet.addAction(file)
        }

        if (self.allowAudioRecorder) {
            let audiorecorder = UIAlertAction(title: Constants.audiorecorder, style: .default, handler: { (action) -> Void in
                self.audiorecorder()
            })
            actionSheet.addAction(audiorecorder)
        }

        if (self.allowVideoRecorder) {
            let videorecorder = UIAlertAction(title: Constants.videorecorder, style: .default, handler: { (action) -> Void in
                self.videorecorder()
            })
            actionSheet.addAction(videorecorder)
        }

        let cancel = UIAlertAction(title: Constants.cancel, style: .cancel, handler: nil)
        actionSheet.addAction(cancel)
        
        // fix for ipad from: https://stackoverflow.com/a/54932223
    //    actionSheet.popoverPresentationController?.sourceView = currentViewController
    //    actionSheet.popoverPresentationController?.sourceRect = currentViewController.bounds
    //    // or maybe: actionSheet.popoverPresentationController?.sourceRect = CGRect(x: currentViewController.bounds.midX, y: currentViewController.bounds.midY, width: 0, height: 0)
    //    actionSheet.popoverPresentationController?.permittedArrowDirections = []

        

        viewController.present(actionSheet, animated: true, completion: nil)
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

    func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
        currentViewController.dismiss(animated: true, completion: nil)
    }
    func imagePickerController(_ picker: UIImagePickerController, didFinishPickingMediaWithInfo info: [String : Any]) {
        self.lastInfo = info;

        if let image = info[UIImagePickerControllerImageURL] as? URL {
            imagePickerBlock?(image) //return image when not null
        }
        else if let videoUrl = info[UIImagePickerControllerMediaURL] as? URL {
            //let data = try? Data(contentsOf: videoUrl)
            videoPickerBlock?(videoUrl) //return video url when not null
        }
        else if let image = info[UIImagePickerControllerOriginalImage] as? UIImage {
            let strBase64:String = UIImageJPEGRepresentation(image, 1)?.base64EncodedString() ?? ""
            cameraPickerBlock?(strBase64) //return image when not null
        }
        else{
            print("Something went wrong")
        }
        currentViewController.dismiss(animated: true, completion: nil)
    }
}

extension CordovaMediaPicker: UIDocumentPickerDelegate  {
    func documentMenu(_ documentMenu: UIDocumentPickerViewController, didPickDocumentPicker documentPicker: UIDocumentPickerViewController) {
        documentPicker.delegate = self
        currentViewController.present(documentPicker, animated: true, completion: nil)
    }
    func documentPicker(_ controller: UIDocumentPickerViewController,   didPickDocumentAt url: URL) {
        filePickerBlock?(url) //return file url if you selected from drive.
    }
    func documentMenuWasCancelled(_ documentMenu: UIDocumentPickerViewController) {
        currentViewController.dismiss(animated: true, completion: nil)
    }
}