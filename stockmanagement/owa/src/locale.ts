import i18next from "i18next";
import { initReactI18next } from "react-i18next";
import translationEN from "./locales/en.json";

const resources = {
  en: {
    translation: translationEN
  }
};

export function setupI18n() {    
  return i18next
    .use(initReactI18next)
    .init({      
        resources: resources,
        fallbackLng: "en",      
        lng: (window as any).sessionContext?.locale ?? "en"
    });
}
