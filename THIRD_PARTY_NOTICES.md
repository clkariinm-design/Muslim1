# Nuur Qur’an — Third-Party Notices

This file is an inventory based on the project README. It is not a substitute for checking the exact files bundled in the release, their upstream licence texts, attribution requirements, and any additional restrictions.

## Datasets and content identified by the project

| Material | Upstream/source stated in README | Licence/terms stated in README | Required action before release |
|---|---|---|---|
| Uthmani Qur’an text | AlQuran Cloud / Tanzil | CC BY 3.0 / Tanzil attribution | Confirm exact dataset provenance and version; include required attribution and licence notice with the distributed data. |
| Tajweed text/rules | AlQuran Cloud / Tanzil | CC BY 3.0 / Tanzil attribution | Verify exact dataset licence and attribution requirements for the bundled files. |
| Somali translation (`so.abduh`) | AlQuran Cloud / Tanzil | CC BY 3.0 / Tanzil attribution | Verify translator/source attribution and whether the stated licence covers this exact translation. |
| English translation (`en.sahih`) | AlQuran Cloud / Tanzil | CC BY 3.0 / Tanzil attribution | Verify exact edition, attribution, and licence conditions. |
| Arabic Tafsir Al-Muyassar (`ar.muyassar`) | AlQuran Cloud / Tanzil | CC BY 3.0 / Tanzil attribution | Verify the tafsir publisher/rightsholder and exact redistribution terms; do not assume all tafsir text is covered by the same licence. |
| Morning and Evening Adhkar | `Seen-Arabic/Morning-And-Evening-Adhkar-DB` | MIT, as stated in README | Include the upstream MIT copyright and permission notice for the exact version used. Verify Arabic/English text provenance separately. |
| Audio recitations | Islamic Network CDN | AlAdhan states that audio files retain their respective copyrights; its community guidance says the network imposes no additional usage restriction, but does not grant rights on behalf of reciters. | Default Adhan source: Ahmad al-Nafees, served from https://cdn.aladhan.com/audio/adhans/a1.mp3 and cached privately in app storage. Retain source/reciter attribution and verify the recording rightsholder's terms before commercial redistribution. |
| Optional online Tafsir Ibn Kathir | Quran.com API v4 | API/service terms not recorded here | Review current API terms, attribution rules, caching limits, and permitted use. |

## Android libraries and build plugins

The app uses dependencies declared through Gradle version catalogs and plugins. Generate a complete dependency inventory (including transitive dependencies) from the exact release build and include the licences/notices required by each dependency. Do not assume that the libraries all use one licence.

## Attribution checklist

- Preserve required author, translator, publisher, source, copyright, and licence notices.
- Provide a clear in-app or bundled “Acknowledgements / Licences” screen or document.
- Keep a copy of the exact upstream licence files and dataset versions used.
- Record URLs and retrieval dates for every external dataset and audio source.
- Check that the licence permits commercial use, modification, redistribution, and offline bundling as applicable.
- Do not sell or describe third-party content as exclusively owned by the app developer.
- If a licence is unclear, replace the material or obtain written permission before commercial distribution.

## Important

The README's licence labels are unverified claims until matched to the actual bundled files and upstream licence texts. This inventory is not legal advice.
