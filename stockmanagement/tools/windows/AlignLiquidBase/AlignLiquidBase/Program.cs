using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Xml;

namespace ConsoleApplication3
{
    class Program
    {
        static void Main(string[] args)
        {
            var xmlDocument = new XmlDocument();
            xmlDocument.Load(@"F:\workspace\ugandaemr\komusoft22\mets\stockmanagement\api\src\main\resources\liquibase.xml");
            var ns = new XmlNamespaceManager(xmlDocument.NameTable);
            ns.AddNamespace("ns", "http://www.liquibase.org/xml/ns/dbchangelog/1.9");
            var i = 1;
            foreach (XmlNode xmlNode in xmlDocument.DocumentElement.SelectNodes("//ns:changeSet",ns))
            {
                xmlNode.Attributes["id"].Value = $"stockmanagement-1758855877841-{i++}";
            }
            
            xmlDocument.Save(@"F:\workspace\ugandaemr\komusoft22\mets\stockmanagement\api\src\main\resources\liquibase.xml");
        }
    }
}
