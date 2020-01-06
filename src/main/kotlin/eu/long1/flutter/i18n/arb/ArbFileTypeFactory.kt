package eu.long1.flutter.i18n.arb

import com.intellij.openapi.fileTypes.FileTypeFactory
import com.intellij.openapi.fileTypes.FileTypeConsumer

class ArbFileTypeFactory : FileTypeFactory() {
    override fun createFileTypes(consumer: FileTypeConsumer) =
        consumer.consume(ArbFileType, ArbFileType.defaultExtension)
}