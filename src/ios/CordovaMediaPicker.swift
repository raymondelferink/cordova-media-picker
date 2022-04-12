import UIKit
import MobileCoreServices
import Foundation

@objc(CordovaMediaPicker) class CordovaMediaPicker : CDVPlugin {
    var commandCallback: String?
    var allowCamera = true
    var lastInfo: [String : Any]?

    enum MediaType {
        case image
        case media
        case all
    }
    struct Constants {
       static let camera = "Camera"
       static let gallery = "Gallery"
       static let video = "Video"
       static let file = "File"
       static let cancel = "Cancel"
       static let documentTypes = ["com.microsoft.word.doc", "public.data", "org.openxmlformats.wordprocessingml.document", kUTTypePDF as String] //Use for specify type you need to pickup
    }

    static let shared: CordovaMediaPicker = CordovaMediaPicker() //Singleton Pattern
    fileprivate var currentViewController: UIViewController!
    var cameraPickerBlock: ((_ base64: String) -> Void)?
    var imagePickerBlock: ((_ image: URL) -> Void)?
    var videoPickerBlock: ((_ data: URL) -> Void)?
    var filePickerBlock: ((_ url: URL) -> Void)?

    func callPicker (options: NSDictionary) {
        
        if (options["camera"] != nil) {
            self.allowCamera = (options["camera"] as! Bool);
        } else {
            self.allowCamera = true;
        }

        self.showActionSheet(viewController: self.viewController, type: .all)
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
    }
    @objc(pick:)
    func pick(command: CDVInvokedUrlCommand) {
        self.commandCallback = command.callbackId
        let options = command.arguments.first as! NSDictionary

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
       let importMenuViewController = UIDocumentPickerViewController(documentTypes: Constants.documentTypes, in: .import)
       importMenuViewController.delegate = self
       importMenuViewController.modalPresentationStyle = .formSheet
       currentViewController.present(importMenuViewController, animated: true, completion: nil)
    }

    func showActionSheet(viewController: UIViewController, type: MediaType) {
        currentViewController = viewController
        let actionSheet = UIAlertController(title: nil, message: nil, preferredStyle: .actionSheet)

        if (self.allowCamera) {
            let camera = UIAlertAction(title: Constants.camera, style: .default, handler: { (action) -> Void in
               self.camera()
            })
            actionSheet.addAction(camera)
        }


        let gallery = UIAlertAction(title: Constants.gallery, style: .default, handler: { (action) -> Void in
            self.photoLibrary()
        })
        actionSheet.addAction(gallery)

        let video = UIAlertAction(title: Constants.video, style: .default, handler: { (action) -> Void in
            self.video()
        })
        let file = UIAlertAction(title: Constants.file, style: .default, handler: { (action) -> Void in
            self.file()
        })
        if type == .media {
           actionSheet.addAction(video)
        }
        else if type == .all {
           actionSheet.addAction(video)
           actionSheet.addAction(file)
        }

        let cancel = UIAlertAction(title: Constants.cancel, style: .cancel, handler: nil)
        actionSheet.addAction(cancel)

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