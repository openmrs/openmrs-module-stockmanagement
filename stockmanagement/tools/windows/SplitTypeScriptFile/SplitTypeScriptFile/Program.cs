using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Text.RegularExpressions;
using System.Threading.Tasks;

namespace SplitTypeScriptFile
{
    class Program
    {
        private static readonly Regex parser = new Regex(@"interface(?<interface>.*)\s*extends\s*(?<superinterface>.*)\s*{", RegexOptions.IgnoreCase | RegexOptions.Singleline | RegexOptions.Compiled);
        private static readonly Regex parser2 = new Regex(@"interface(?<interface>.*)\s*{", RegexOptions.IgnoreCase | RegexOptions.Singleline | RegexOptions.Compiled);
        static void Main(string[] args)
        {
            try
            {
                if (args.Length < 2) return;

                if (!Directory.Exists(args[0]))
                    Directory.CreateDirectory(args[0]);

                var spliter = new char[] {' ', ','};


                foreach (var file in args.Skip(1))
                {
                    string text = File.ReadAllText(file);
                    var startIndex = 0;
                    var last = false;
                    while ((startIndex = text.IndexOf("export interface", startIndex)) >= 0)
                    {

                        var nextIndex = text.IndexOf("export interface", startIndex + 14);
                        string body = null;
                        
                        if (nextIndex > 0)
                        {
                            body = text.Substring(startIndex, nextIndex - startIndex);
                        }
                        else
                        {
                            last = true;
                            body = text.Substring(startIndex);
                        }

                        var matches = parser.Matches(body);
                        if (matches.Count != 1)
                        {
                            matches = parser2.Matches(body);
                            if (matches.Count != 1)
                            {
                                startIndex = last ? body.Length - 1 : nextIndex;
                                continue;
                            }
                            
                        }

                        var match = matches[0];
                        var interfaceName = match.Groups["interface"].Value.Trim();
                        if(match.Groups.Count > 1)
                        { 
                            var super = match.Groups["superinterface"].Value.Trim().Split(spliter, StringSplitOptions.RemoveEmptyEntries).Where(p=> !"Serializable".Equals(p,StringComparison.CurrentCultureIgnoreCase)).ToArray();

                            var fileName = toFileName(interfaceName);

                            File.WriteAllText(Path.Combine(args[0],$"{fileName}.ts"), string.Join(Environment.NewLine,
                                super.Select(p =>
                                {
                                    return $"import {{ {p} }} from './{toFileName(p)}'";
                                })) + Environment.NewLine + Environment.NewLine +
                                body.Replace(", Serializable",string.Empty).Replace("Serializable","")
                                );

                         }
                        else
                        {
                            foreach (var matchGroup in match.Groups)
                            {
                                
                            }
                        }
                        startIndex = last ? body.Length - 1 : nextIndex;

                    }

                }
            }
            catch (Exception exception)
            {
                
            }
        }

        private static string toFileName(string filePath)
        {
            return filePath; // string.Join("",filePath.ToCharArray().Select((c, i) => i == 0 ? Char.ToLower(c) : c));
        }
    }
}
