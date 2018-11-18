package eu.long1.flutter.i18n.arb

import com.intellij.json.JsonFileTypeFactory
import com.intellij.openapi.fileTypes.FileTypeConsumer

class ArbFileTypeFactory : JsonFileTypeFactory() {
    override fun createFileTypes(consumer: FileTypeConsumer) =
        consumer.consume(ArbFileType, ArbFileType.defaultExtension)
}